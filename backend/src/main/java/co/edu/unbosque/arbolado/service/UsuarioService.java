package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.CrearProfesorRequest;
import co.edu.unbosque.arbolado.dto.PaginaDto;
import co.edu.unbosque.arbolado.dto.UsuarioDto;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.ConflictoException;
import co.edu.unbosque.arbolado.exception.NoAutenticadoException;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Administración de usuarios: creación de profesores, listados, consulta y perfil propio (RF-10, RF-13, RF-14). */
@Service
public class UsuarioService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "USUARIO";

	/** Tamaño de página por defecto y máximo permitido. */
	static final int TAMANO_POR_DEFECTO = 20;

	/** Máximo de elementos por página. */
	static final int TAMANO_MAXIMO = 100;

	/** Carácter que escapa los comodines del texto de búsqueda. */
	private static final char ESCAPE = '!';

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	private final CodificadorContrasena codificador;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarios    repositorio de usuarios
	 * @param codificador codificador de contraseñas
	 * @param auditoria   registro de auditoría
	 * @param mapper      conversor de entidades a DTO
	 */
	public UsuarioService(UsuarioRepository usuarios, CodificadorContrasena codificador, AuditoriaService auditoria,
						  ModelMapper mapper) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.auditoria = auditoria;
		this.mapper = mapper;
	}

	/**
	 * Crea un profesor activo, sin verificación de correo (RN-05). El documento y el correo deben ser
	 * únicos en todo el sistema, incluso frente a cuentas pendientes.
	 *
	 * @param solicitud datos del profesor
	 * @param admin     administrador que lo crea, tomado del token
	 * @return el profesor creado
	 * @throws ConflictoException si el documento o el correo ya pertenecen a otra cuenta
	 */
	@Transactional
	public UsuarioDto crearProfesor(CrearProfesorRequest solicitud, UsuarioAutenticado admin) {
		String correo = solicitud.getCorreo().trim().toLowerCase(Locale.ROOT);
		if (usuarios.existsByDocumento(solicitud.getDocumento())) {
			throw rechazar(admin, CodigoError.DOCUMENTO_DUPLICADO, "Ya existe una cuenta con ese documento");
		}
		if (usuarios.existsByCorreo(correo)) {
			throw rechazar(admin, CodigoError.CORREO_DUPLICADO, "Ya existe una cuenta con ese correo");
		}

		Usuario profesor;
		try {
			profesor = usuarios.saveAndFlush(new Usuario(solicitud.getDocumento(), solicitud.getNombreCompleto().trim(),
					correo, codificador.encode(solicitud.getContrasena()), Rol.PROFESOR, EstadoCuenta.ACTIVO));
		} catch (DataIntegrityViolationException e) {
			// Otra petición ganó la carrera entre la consulta y el guardado: lo decide la restricción única.
			throw rechazar(admin, CodigoError.DOCUMENTO_DUPLICADO, "El documento o el correo ya están registrados");
		}
		auditoria.registrarExitoso(AccionAuditoria.CREACION_PROFESOR, admin.id(), admin.correo(), ENTIDAD,
				String.valueOf(profesor.getId()), "Profesor creado: " + profesor.getCorreo());
		return mapper.map(profesor, UsuarioDto.class);
	}

	/**
	 * Lista las cuentas de un rol, con búsqueda opcional y orden por nombre.
	 *
	 * @param rol    rol a listar
	 * @param q      texto a buscar en nombre, documento o correo; vacío o {@code null} lista todas
	 * @param pagina página pedida; un valor negativo se ajusta a 0
	 * @param tamano elementos por página; se ajusta al rango de 1 a 100
	 * @return la página de cuentas
	 */
	@Transactional(readOnly = true)
	public PaginaDto<UsuarioDto> listar(Rol rol, String q, int pagina, int tamano) {
		int paginaAjustada = Math.max(pagina, 0);
		int tamanoAjustado = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
		PageRequest paginado = PageRequest.of(paginaAjustada, tamanoAjustado,
				Sort.by("nombreCompleto").ascending().and(Sort.by("id").ascending()));

		Page<Usuario> resultado = usuarios.buscar(rol, prepararBusqueda(q), paginado);
		return new PaginaDto<>(resultado.getContent().stream().map(u -> mapper.map(u, UsuarioDto.class)).toList(),
				resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
	}

	/**
	 * Consulta una cuenta de un rol. Un identificador de otro rol responde igual que uno inexistente,
	 * para que {@code /profesores/{id}} no sirva para consultar estudiantes.
	 *
	 * @param rol rol esperado
	 * @param id  identificador de la cuenta
	 * @return la cuenta
	 * @throws NoEncontradoException si no existe una cuenta de ese rol con ese identificador
	 */
	@Transactional(readOnly = true)
	public UsuarioDto obtener(Rol rol, Long id) {
		return usuarios.findById(id)
				.filter(u -> u.getRol() == rol)
				.map(u -> mapper.map(u, UsuarioDto.class))
				.orElseThrow(() -> new NoEncontradoException("No existe un " + rol.name().toLowerCase(Locale.ROOT)
						+ " con ese identificador"));
	}

	/**
	 * Devuelve la cuenta de quien hace la petición (solo lectura, decisión A-10).
	 *
	 * @param actual identidad tomada del token
	 * @return los datos de la cuenta
	 * @throws NoAutenticadoException si la cuenta del token ya no existe
	 */
	@Transactional(readOnly = true)
	public UsuarioDto perfil(UsuarioAutenticado actual) {
		return usuarios.findById(actual.id())
				.map(u -> mapper.map(u, UsuarioDto.class))
				.orElseThrow(() -> new NoAutenticadoException(CodigoError.NO_AUTENTICADO, "La sesión ya no es válida"));
	}

	/**
	 * Deja el texto de búsqueda en minúsculas, sin espacios en los extremos y con los comodines de LIKE
	 * escapados: así "50%" o "_" se buscan como texto y no como patrón.
	 *
	 * @param q texto recibido, puede ser {@code null}
	 * @return el texto listo para la consulta; vacío si no hay filtro
	 */
	static String prepararBusqueda(String q) {
		if (q == null) {
			return "";
		}
		String limpio = q.trim().toLowerCase(Locale.ROOT);
		StringBuilder escapado = new StringBuilder(limpio.length());
		for (char c : limpio.toCharArray()) {
			if (c == ESCAPE || c == '%' || c == '_') {
				escapado.append(ESCAPE);
			}
			escapado.append(c);
		}
		return escapado.toString();
	}

	/**
	 * Audita una creación rechazada y arma el conflicto.
	 *
	 * @param admin   administrador que lo intentó
	 * @param codigo  código de la familia 409
	 * @param mensaje explicación para la persona
	 * @return la excepción lista para lanzar
	 */
	private ConflictoException rechazar(UsuarioAutenticado admin, CodigoError codigo, String mensaje) {
		auditoria.registrarFallido(AccionAuditoria.CREACION_PROFESOR, admin.id(), admin.correo(), ENTIDAD, null, mensaje);
		return new ConflictoException(codigo, mensaje);
	}
}
