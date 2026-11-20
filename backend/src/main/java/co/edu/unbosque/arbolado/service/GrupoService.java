package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.AsignaturaResumenDto;
import co.edu.unbosque.arbolado.dto.GrupoDto;
import co.edu.unbosque.arbolado.dto.GrupoRequest;
import co.edu.unbosque.arbolado.dto.HorarioDto;
import co.edu.unbosque.arbolado.dto.PeriodoDto;
import co.edu.unbosque.arbolado.dto.UsuarioResumenDto;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.ConflictoException;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.exception.ReglaNegocioException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.DiaSemana;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Horario;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import co.edu.unbosque.arbolado.repository.GrupoRepository;
import co.edu.unbosque.arbolado.repository.PeriodoRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Grupos y horarios: creación y edición por el administrador y consultas de oferta. */
@Service
public class GrupoService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "GRUPO";

	/** Repositorio de grupos. */
	private final GrupoRepository grupos;

	/** Repositorio de asignaturas. */
	private final AsignaturaRepository asignaturas;

	/** Repositorio de periodos. */
	private final PeriodoRepository periodos;

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Conteo de inscripciones por grupo. */
	private final InscritosDeGrupo inscritos;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Franja ya interpretada: el día y las horas como {@link LocalTime}.
	 *
	 * @param dia    día de la semana
	 * @param inicio hora de inicio
	 * @param fin    hora de fin
	 */
	record Franja(
			DiaSemana dia,
			LocalTime inicio,
			LocalTime fin) {

		/**
		 * Indica si dos franjas se pisan. Empezar justo cuando otra termina no es traslape.
		 *
		 * @param otra la otra franja
		 * @return {@code true} si comparten algún minuto del mismo día
		 */
		boolean seTraslapaCon(final Franja otra) {
			return dia == otra.dia() && inicio.isBefore(otra.fin()) && otra.inicio().isBefore(fin);
		}
	}

	/**
	 * Crea el servicio.
	 *
	 * @param grupos      repositorio de grupos
	 * @param asignaturas repositorio de asignaturas
	 * @param periodos    repositorio de periodos
	 * @param usuarios    repositorio de usuarios
	 * @param inscritos   conteo de inscripciones por grupo
	 * @param auditoria   registro de auditoría
	 * @param mapper      conversor de entidades a DTO
	 */
	public GrupoService(GrupoRepository grupos, AsignaturaRepository asignaturas, PeriodoRepository periodos,
						UsuarioRepository usuarios, InscritosDeGrupo inscritos, AuditoriaService auditoria,
						ModelMapper mapper) {
		this.grupos = grupos;
		this.asignaturas = asignaturas;
		this.periodos = periodos;
		this.usuarios = usuarios;
		this.inscritos = inscritos;
		this.auditoria = auditoria;
		this.mapper = mapper;
	}

	/**
	 * Crea un grupo con el siguiente número libre de su asignatura y periodo, y con cupo de 30.
	 *
	 * @param solicitud datos del grupo
	 * @param admin     administrador que lo crea
	 * @return el grupo creado
	 * @throws NoEncontradoException si la asignatura, el periodo o el profesor no existen
	 * @throws ReglaNegocioException si el responsable no es profesor o los horarios son inválidos
	 * @throws ConflictoException    si otra petición tomó el mismo número al mismo tiempo
	 */
	@Transactional
	public GrupoDto crear(GrupoRequest solicitud, UsuarioAutenticado admin) {
		Asignatura asignatura = asignaturas.findById(solicitud.getAsignaturaId())
				.orElseThrow(() -> NoEncontradoException.de("La asignatura", solicitud.getAsignaturaId()));
		Periodo periodo = periodos.findById(solicitud.getPeriodoId())
				.orElseThrow(() -> NoEncontradoException.de("El periodo", solicitud.getPeriodoId()));
		Usuario profesor = buscarProfesor(solicitud.getProfesorId(), AccionAuditoria.CREACION_GRUPO, admin, null);
		List<Franja> franjas = interpretar(solicitud.getHorarios(), AccionAuditoria.CREACION_GRUPO, admin, null);

		Grupo grupo = new Grupo(grupos.siguienteNumero(asignatura.getId(), periodo.getId()), asignatura, periodo,
				profesor, solicitud.getBloque().trim(), solicitud.getSalon().trim());
		grupo.reemplazarHorarios(aEntidades(grupo, franjas));
		try {
			grupos.saveAndFlush(grupo);
		} catch (DataIntegrityViolationException e) {
			// Dos creaciones simultáneas calcularon el mismo número; la restricción única decide.
			throw rechazarNumero(AccionAuditoria.CREACION_GRUPO, admin, null);
		}
		auditoria.registrarExitoso(AccionAuditoria.CREACION_GRUPO, admin.id(), admin.correo(), ENTIDAD,
				String.valueOf(grupo.getId()), "Grupo " + asignatura.getCodigo() + "-" + grupo.getNumero() + " del periodo "
						+ periodo.getCodigo());
		return aDto(grupo, 0L);
	}

	/**
	 * Edita un grupo. Siempre se pueden cambiar profesor, bloque y salón; la asignatura, el periodo y los
	 * horarios solo mientras no haya inscripciones. El grupo se bloquea durante la edición
	 * para que no se inscriba nadie a la vez.
	 *
	 * @param id        grupo a editar
	 * @param solicitud datos nuevos
	 * @param admin     administrador que lo edita
	 * @return el grupo editado
	 * @throws NoEncontradoException si el grupo, la asignatura, el periodo o el profesor no existen
	 * @throws ReglaNegocioException si hay inscritos y se intenta cambiar lo bloqueado, o los datos son inválidos
	 * @throws ConflictoException    si el número nuevo choca con otro grupo
	 */
	@Transactional
	public GrupoDto editar(Long id, GrupoRequest solicitud, UsuarioAutenticado admin) {
		Grupo grupo = grupos.findConBloqueoById(id).orElseThrow(() -> NoEncontradoException.de("El grupo", id));
		Asignatura asignatura = asignaturas.findById(solicitud.getAsignaturaId())
				.orElseThrow(() -> NoEncontradoException.de("La asignatura", solicitud.getAsignaturaId()));
		Periodo periodo = periodos.findById(solicitud.getPeriodoId())
				.orElseThrow(() -> NoEncontradoException.de("El periodo", solicitud.getPeriodoId()));
		Usuario profesor = buscarProfesor(solicitud.getProfesorId(), AccionAuditoria.MODIFICACION_GRUPO, admin, id);
		List<Franja> franjas = interpretar(solicitud.getHorarios(), AccionAuditoria.MODIFICACION_GRUPO, admin, id);

		long conInscripciones = inscritos.contarInscripciones(id);
		boolean cambiaEstructura = !grupo.getAsignatura().getId().equals(asignatura.getId())
				|| !grupo.getPeriodo().getId().equals(periodo.getId())
				|| !franjasDe(grupo).equals(Set.copyOf(franjas));
		if (conInscripciones > 0 && cambiaEstructura) {
			String mensaje = "El grupo ya tiene " + conInscripciones + " inscripciones: solo puedes cambiar el profesor, "
					+ "el bloque y el salón";
			auditoria.registrarFallido(AccionAuditoria.MODIFICACION_GRUPO, admin.id(), admin.correo(), ENTIDAD,
					String.valueOf(id), "Edición rechazada: " + mensaje);
			throw new ReglaNegocioException(CodigoError.REGLA_NEGOCIO, mensaje);
		}

		grupo.actualizarUbicacion(profesor, solicitud.getBloque().trim(), solicitud.getSalon().trim());
		if (cambiaEstructura) {
			boolean cambiaOferta = !grupo.getAsignatura().getId().equals(asignatura.getId())
					|| !grupo.getPeriodo().getId().equals(periodo.getId());
			if (cambiaOferta) {
				grupo.reubicar(asignatura, periodo, grupos.siguienteNumero(asignatura.getId(), periodo.getId()));
			}
			grupo.reemplazarHorarios(aEntidades(grupo, franjas));
		}
		try {
			grupos.saveAndFlush(grupo);
		} catch (DataIntegrityViolationException e) {
			throw rechazarNumero(AccionAuditoria.MODIFICACION_GRUPO, admin, id);
		}
		auditoria.registrarExitoso(AccionAuditoria.MODIFICACION_GRUPO, admin.id(), admin.correo(), ENTIDAD,
				String.valueOf(id), "Grupo " + asignatura.getCodigo() + "-" + grupo.getNumero() + " del periodo "
						+ periodo.getCodigo() + (cambiaEstructura ? " (con cambios de oferta u horario)" : ""));
		return aDto(grupo, cursando(List.of(grupo)).getOrDefault(id, 0L));
	}

	/**
	 * Lista los grupos, de todos los periodos o de uno.
	 *
	 * @param periodoId periodo a filtrar, o {@code null} para todos
	 * @return los grupos con cupos
	 */
	@Transactional(readOnly = true)
	public List<GrupoDto> listar(Long periodoId) {
		return aDtos(periodoId == null ? grupos.findAllByOrderByPeriodoCodigoDescAsignaturaCodigoAscNumeroAsc()
				: grupos.findByPeriodoIdOrderByAsignaturaCodigoAscNumeroAsc(periodoId));
	}

	/**
	 * Consulta un grupo.
	 *
	 * @param id identificador
	 * @return el grupo con cupos
	 * @throws NoEncontradoException si no existe
	 */
	@Transactional(readOnly = true)
	public GrupoDto obtener(Long id) {
		Grupo grupo = grupos.findConDetalleById(id).orElseThrow(() -> NoEncontradoException.de("El grupo", id));
		return aDto(grupo, cursando(List.of(grupo)).getOrDefault(id, 0L));
	}

	/**
	 * Oferta del periodo activo con los cupos disponibles de cada grupo.
	 *
	 * @return los grupos del periodo activo; vacío si no hay periodo activo
	 */
	@Transactional(readOnly = true)
	public List<GrupoDto> oferta() {
		return periodos.findByActivoTrue()
				.map(p -> aDtos(grupos.findByPeriodoIdOrderByAsignaturaCodigoAscNumeroAsc(p.getId())))
				.orElseGet(List::of);
	}

	/**
	 * Grupos de los que es responsable un profesor. El profesor sale del token, no de un parámetro.
	 *
	 * @param profesorId profesor autenticado
	 * @param periodoId  periodo a filtrar, o {@code null} para todos
	 * @return los grupos con cupos
	 */
	@Transactional(readOnly = true)
	public List<GrupoDto> delProfesor(Long profesorId, Long periodoId) {
		return aDtos(periodoId == null
				? grupos.findByProfesorIdOrderByPeriodoCodigoDescAsignaturaCodigoAscNumeroAsc(profesorId)
				: grupos.findByProfesorIdAndPeriodoIdOrderByAsignaturaCodigoAscNumeroAsc(profesorId, periodoId));
	}

	/**
	 * Convierte un grupo ya cargado en su DTO, contando sus cupos en ese momento. Lo usan los servicios
	 * que devuelven un grupo dentro de otro recurso (por ejemplo la inscripción).
	 *
	 * @param grupo grupo con asignatura, periodo, profesor y horarios accesibles
	 * @return el DTO con cupos
	 */
	@Transactional(readOnly = true)
	public GrupoDto detalle(Grupo grupo) {
		return aDto(grupo, cursando(List.of(grupo)).getOrDefault(grupo.getId(), 0L));
	}

	/**
	 * Convierte varios grupos ya cargados en DTO con una sola consulta de cupos, para no contar grupo por
	 * grupo al listar inscripciones.
	 *
	 * @param lista grupos con detalle; puede repetir grupos
	 * @return DTO por identificador de grupo
	 */
	@Transactional(readOnly = true)
	public Map<Long, GrupoDto> detalles(Collection<Grupo> lista) {
		List<Grupo> unicos = lista.stream().distinct().toList();
		Map<Long, Long> conteo = cursando(unicos);
		Map<Long, GrupoDto> resultado = new HashMap<>();
		for (Grupo grupo : unicos) {
			resultado.put(grupo.getId(), aDto(grupo, conteo.getOrDefault(grupo.getId(), 0L)));
		}
		return resultado;
	}

	/**
	 * Grupos de un profesor en el periodo activo, para armar su horario.
	 *
	 * @param profesorId profesor autenticado
	 * @return los grupos con horarios; vacío si no hay periodo activo
	 */
	@Transactional(readOnly = true)
	public List<Grupo> delProfesorEnPeriodoActivo(Long profesorId) {
		return periodos.findByActivoTrue()
				.map(p -> grupos.findByProfesorIdAndPeriodoIdOrderByAsignaturaCodigoAscNumeroAsc(profesorId, p.getId()))
				.orElseGet(List::of);
	}

	/**
	 * Busca al responsable y comprueba que sea profesor; un estudiante o administrador no puede dictar grupos.
	 *
	 * @param profesorId identificador pedido
	 * @param accion     acción que se audita si se rechaza
	 * @param admin      administrador que actúa
	 * @param grupoId    grupo afectado, o {@code null} si se está creando
	 * @return el profesor
	 */
	private Usuario buscarProfesor(Long profesorId, AccionAuditoria accion, UsuarioAutenticado admin, Long grupoId) {
		Usuario usuario = usuarios.findById(profesorId).orElseThrow(() -> NoEncontradoException.de("El profesor", profesorId));
		if (usuario.getRol() != Rol.PROFESOR) {
			throw rechazarRegla(accion, admin, grupoId, "La persona elegida no tiene el rol de profesor");
		}
		return usuario;
	}

	/**
	 * Convierte los horarios recibidos en franjas y valida que cada una termine después de empezar y que no
	 * se pisen entre sí.
	 *
	 * @param recibidos horarios del cuerpo de la petición
	 * @param accion    acción que se audita si se rechaza
	 * @param admin     administrador que actúa
	 * @param grupoId   grupo afectado, o {@code null} si se está creando
	 * @return las franjas interpretadas
	 */
	private List<Franja> interpretar(List<HorarioDto> recibidos, AccionAuditoria accion, UsuarioAutenticado admin,
									 Long grupoId) {
		List<Franja> franjas = new ArrayList<>();
		for (HorarioDto h : recibidos) {
			Franja franja = new Franja(h.getDiaSemana(), LocalTime.parse(h.getHoraInicio()), LocalTime.parse(h.getHoraFin()));
			if (!franja.inicio().isBefore(franja.fin())) {
				throw rechazarRegla(accion, admin, grupoId, "En el horario del " + franja.dia()
						+ " la hora de fin debe ser posterior a la de inicio");
			}
			for (Franja previa : franjas) {
				if (previa.seTraslapaCon(franja)) {
					throw rechazarRegla(accion, admin, grupoId, "Dos horarios del grupo se traslapan el " + franja.dia());
				}
			}
			franjas.add(franja);
		}
		return franjas;
	}

	/**
	 * Franjas actuales del grupo como conjunto, para compararlas sin importar el orden.
	 *
	 * @param grupo grupo guardado
	 * @return las franjas
	 */
	private static Set<Franja> franjasDe(Grupo grupo) {
		return grupo.getHorarios().stream()
				.map(h -> new Franja(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
				.collect(Collectors.toSet());
	}

	/**
	 * Convierte franjas en entidades de un grupo.
	 *
	 * @param grupo   grupo dueño
	 * @param franjas franjas ya validadas
	 * @return las entidades
	 */
	private static List<Horario> aEntidades(Grupo grupo, List<Franja> franjas) {
		return franjas.stream().map(f -> new Horario(grupo, f.dia(), f.inicio(), f.fin())).toList();
	}

	/**
	 * Cuenta los estudiantes cursando de varios grupos con una sola consulta.
	 *
	 * @param lista grupos a contar
	 * @return cantidad por identificador de grupo
	 */
	private Map<Long, Long> cursando(List<Grupo> lista) {
		return inscritos.cursandoPorGrupo(lista.stream().map(Grupo::getId).toList());
	}

	/**
	 * Convierte una lista de grupos en DTO con sus cupos.
	 *
	 * @param lista grupos con detalle
	 * @return los DTO
	 */
	private List<GrupoDto> aDtos(List<Grupo> lista) {
		Map<Long, Long> cursando = cursando(lista);
		return lista.stream().map(g -> aDto(g, cursando.getOrDefault(g.getId(), 0L))).toList();
	}

	/**
	 * Convierte un grupo en su DTO, con horarios ordenados por día y hora y cupos calculados.
	 *
	 * @param grupo    grupo con detalle
	 * @param cursando estudiantes cursando
	 * @return el DTO
	 */
	private GrupoDto aDto(Grupo grupo, long cursando) {
		GrupoDto dto = new GrupoDto();
		dto.setId(grupo.getId());
		dto.setNumero(grupo.getNumero());
		dto.setAsignatura(mapper.map(grupo.getAsignatura(), AsignaturaResumenDto.class));
		dto.setPeriodo(mapper.map(grupo.getPeriodo(), PeriodoDto.class));
		dto.setProfesor(mapper.map(grupo.getProfesor(), UsuarioResumenDto.class));
		dto.setBloque(grupo.getBloque());
		dto.setSalon(grupo.getSalon());
		dto.setCupoMaximo(grupo.getCupoMaximo());
		dto.setInscritos(cursando);
		dto.setCuposDisponibles(Math.max(0, grupo.getCupoMaximo() - cursando));
		dto.setHorarios(grupo.getHorarios().stream()
				.sorted(Comparator.comparing(Horario::getDiaSemana).thenComparing(Horario::getHoraInicio))
				.map(h -> new HorarioDto(h.getDiaSemana(), h.getHoraInicio().toString(), h.getHoraFin().toString()))
				.toList());
		return dto;
	}

	/**
	 * Audita un rechazo por regla de negocio y arma la excepción.
	 *
	 * @param accion  acción rechazada
	 * @param admin   administrador que actúa
	 * @param grupoId grupo afectado, o {@code null}
	 * @param mensaje explicación para la persona
	 * @return la excepción lista para lanzar
	 */
	private ReglaNegocioException rechazarRegla(AccionAuditoria accion, UsuarioAutenticado admin, Long grupoId,
												String mensaje) {
		auditoria.registrarFallido(accion, admin.id(), admin.correo(), ENTIDAD,
				grupoId == null ? null : String.valueOf(grupoId), mensaje);
		return new ReglaNegocioException(CodigoError.REGLA_NEGOCIO, mensaje);
	}

	/**
	 * Audita un número de grupo repetido y arma el conflicto.
	 *
	 * @param accion  acción rechazada
	 * @param admin   administrador que actúa
	 * @param grupoId grupo afectado, o {@code null}
	 * @return la excepción lista para lanzar
	 */
	private ConflictoException rechazarNumero(AccionAuditoria accion, UsuarioAutenticado admin, Long grupoId) {
		String mensaje = "Otro grupo tomó ese número al mismo tiempo; vuelve a intentarlo";
		auditoria.registrarFallido(accion, admin.id(), admin.correo(), ENTIDAD,
				grupoId == null ? null : String.valueOf(grupoId), mensaje);
		return new ConflictoException(CodigoError.GRUPO_DUPLICADO, mensaje);
	}
}
