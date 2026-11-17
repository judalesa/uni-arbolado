package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.LoginRequest;
import co.edu.unbosque.arbolado.dto.LoginResponse;
import co.edu.unbosque.arbolado.dto.UsuarioDto;
import co.edu.unbosque.arbolado.exception.AccesoDenegadoException;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.NoAutenticadoException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.JwtService;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/** Inicio y cierre de sesión (RF-07, RF-08) con su auditoría (RN-30, RN-31). */
@Service
public class AuthService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "USUARIO";

	/** Mensaje único para correo inexistente o contraseña errónea: no revela cuál falló. */
	private static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos";

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	private final CodificadorContrasena codificador;

	/** Generador de tokens. */
	private final JwtService jwt;

	/** Lista de tokens revocados. */
	private final TokenRevocadoService revocados;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Hash de una clave inexistente. Se compara cuando el correo no existe para que la respuesta tarde
	 * lo mismo que con un correo real y no se pueda deducir qué correos están registrados.
	 */
	private final String hashFicticio;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarios    repositorio de usuarios
	 * @param codificador codificador de contraseñas
	 * @param jwt         generador de tokens
	 * @param revocados   lista de tokens revocados
	 * @param auditoria   registro de auditoría
	 * @param mapper      conversor de entidades a DTO
	 */
	public AuthService(UsuarioRepository usuarios, CodificadorContrasena codificador, JwtService jwt,
					   TokenRevocadoService revocados, AuditoriaService auditoria, ModelMapper mapper) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.jwt = jwt;
		this.revocados = revocados;
		this.auditoria = auditoria;
		this.mapper = mapper;
		this.hashFicticio = codificador.encode("clave-que-no-pertenece-a-nadie");
	}

	/**
	 * Inicia sesión. Primero se verifica la contraseña y solo después el estado de la cuenta, para no
	 * revelar a quien no conoce la clave que una cuenta existe y está pendiente.
	 *
	 * @param solicitud correo y contraseña
	 * @return el token y los datos de la cuenta
	 * @throws NoAutenticadoException   si las credenciales son incorrectas ({@code CREDENCIALES_INVALIDAS})
	 * @throws AccesoDenegadoException  si la cuenta sigue pendiente de verificación ({@code CUENTA_PENDIENTE})
	 */
	@Transactional
	public LoginResponse login(LoginRequest solicitud) {
		String correo = solicitud.getCorreo().trim().toLowerCase(Locale.ROOT);
		Optional<Usuario> encontrado = usuarios.findByCorreo(correo);

		if (encontrado.isEmpty()) {
			codificador.matches(solicitud.getContrasena(), hashFicticio);
			auditoria.registrarFallido(AccionAuditoria.INICIO_SESION, null, correo, ENTIDAD, null,
					"Credenciales inválidas");
			throw new NoAutenticadoException(CodigoError.CREDENCIALES_INVALIDAS, MENSAJE_CREDENCIALES);
		}

		Usuario usuario = encontrado.get();
		if (!codificador.matches(solicitud.getContrasena(), usuario.getContrasena())) {
			auditoria.registrarFallido(AccionAuditoria.INICIO_SESION, usuario.getId(), correo, ENTIDAD,
					String.valueOf(usuario.getId()), "Credenciales inválidas");
			throw new NoAutenticadoException(CodigoError.CREDENCIALES_INVALIDAS, MENSAJE_CREDENCIALES);
		}
		if (usuario.getEstado() == EstadoCuenta.PENDIENTE) {
			auditoria.registrarFallido(AccionAuditoria.INICIO_SESION, usuario.getId(), correo, ENTIDAD,
					String.valueOf(usuario.getId()), "Cuenta pendiente de verificación");
			throw new AccesoDenegadoException(CodigoError.CUENTA_PENDIENTE,
					"Debes verificar tu correo antes de iniciar sesión");
		}

		JwtService.TokenEmitido emitido = jwt.emitir(usuario);
		auditoria.registrarExitoso(AccionAuditoria.INICIO_SESION, usuario.getId(), usuario.getCorreo(), ENTIDAD,
				String.valueOf(usuario.getId()), null);
		return new LoginResponse(emitido.token(), "Bearer", emitido.expiraEn(),
				mapper.map(usuario, UsuarioDto.class));
	}

	/**
	 * Cierra la sesión: el token actual deja de aceptarse hasta su vencimiento.
	 *
	 * @param actual identidad del token con el que se hizo la petición
	 */
	@Transactional
	public void logout(UsuarioAutenticado actual) {
		revocados.revocar(actual.jti(), actual.expiraEn());
		auditoria.registrarExitoso(AccionAuditoria.CIERRE_SESION, actual.id(), actual.correo(), ENTIDAD,
				String.valueOf(actual.id()), null);
	}
}
