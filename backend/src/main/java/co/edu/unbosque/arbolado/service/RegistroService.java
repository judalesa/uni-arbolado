package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.MensajeResponse;
import co.edu.unbosque.arbolado.dto.RegistroRequest;
import co.edu.unbosque.arbolado.dto.ReenviarRequest;
import co.edu.unbosque.arbolado.dto.VerificarRequest;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.ConflictoException;
import co.edu.unbosque.arbolado.exception.DemasiadosIntentosException;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.exception.ReglaNegocioException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/** Registro de estudiantes y verificación del correo (RF-01 a RF-06, RN-01 a RN-04). */
@Service
public class RegistroService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "USUARIO";

	/** Mensaje al enviar un código. */
	private static final String MENSAJE_ENVIADO = "Te enviamos un código de 6 dígitos a tu correo";

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	private final CodificadorContrasena codificador;

	/** Generación y validación de códigos. */
	private final CodigoVerificacionService codigos;

	/** Envío de correos. */
	private final CorreoService correo;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarios    repositorio de usuarios
	 * @param codificador codificador de contraseñas
	 * @param codigos     generación y validación de códigos
	 * @param correo      envío de correos
	 * @param auditoria   registro de auditoría
	 */
	public RegistroService(UsuarioRepository usuarios, CodificadorContrasena codificador,
						   CodigoVerificacionService codigos, CorreoService correo, AuditoriaService auditoria) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.codigos = codigos;
		this.correo = correo;
		this.auditoria = auditoria;
	}

	/**
	 * Registra a un estudiante en estado pendiente y le envía el código. Si el documento o el correo son de
	 * una cuenta pendiente, se actualizan sus datos y se envía un código nuevo (RF-06); si son de una cuenta
	 * activa, se rechaza por duplicado. El correo se envía al final: si falla, todo se revierte y la persona
	 * puede reintentar sin quedar con una cuenta sin código.
	 *
	 * @param solicitud datos del registro
	 * @return mensaje de confirmación
	 * @throws ConflictoException          si el documento o el correo ya pertenecen a otra cuenta
	 * @throws DemasiadosIntentosException si se pide otro código antes de 60 segundos
	 */
	@Transactional
	public MensajeResponse registrar(RegistroRequest solicitud) {
		String documento = solicitud.getDocumento();
		String correoNormal = normalizar(solicitud.getCorreo());
		String nombre = solicitud.getNombreCompleto().trim();

		Optional<Usuario> porDocumento = usuarios.findByDocumento(documento);
		Optional<Usuario> porCorreo = usuarios.findByCorreo(correoNormal);
		if (porDocumento.filter(u -> !esPendiente(u)).isPresent()) {
			throw rechazarRegistro(CodigoError.DOCUMENTO_DUPLICADO, correoNormal, "Ya existe una cuenta con ese documento");
		}
		if (porCorreo.filter(u -> !esPendiente(u)).isPresent()) {
			throw rechazarRegistro(CodigoError.CORREO_DUPLICADO, correoNormal, "Ya existe una cuenta con ese correo");
		}
		if (porDocumento.isPresent() && porCorreo.isPresent()
				&& !porDocumento.get().getId().equals(porCorreo.get().getId())) {
			throw rechazarRegistro(CodigoError.DOCUMENTO_DUPLICADO, correoNormal,
					"El documento y el correo pertenecen a cuentas distintas");
		}

		Optional<Usuario> pendiente = porDocumento.or(() -> porCorreo);
		Usuario usuario;
		if (pendiente.isPresent()) {
			usuario = pendiente.get();
			esperarSiEnvioReciente(usuario, AccionAuditoria.REGISTRO_ESTUDIANTE, correoNormal);
			usuario.actualizarRegistro(documento, nombre, correoNormal, codificador.encode(solicitud.getContrasena()));
		} else {
			try {
				usuario = usuarios.saveAndFlush(new Usuario(documento, nombre, correoNormal,
						codificador.encode(solicitud.getContrasena()), Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE));
			} catch (DataIntegrityViolationException e) {
				throw rechazarRegistro(CodigoError.DOCUMENTO_DUPLICADO, correoNormal,
						"El documento o el correo ya están registrados");
			}
		}

		String id = String.valueOf(usuario.getId());
		auditoria.registrarExitoso(AccionAuditoria.REGISTRO_ESTUDIANTE, usuario.getId(), correoNormal, ENTIDAD, id,
				pendiente.isPresent() ? "Re-registro de cuenta pendiente" : null);
		enviarCodigo(usuario);
		auditoria.registrarExitoso(AccionAuditoria.ENVIO_CODIGO, usuario.getId(), correoNormal, ENTIDAD, id, null);
		return new MensajeResponse(MENSAJE_ENVIADO);
	}

	/**
	 * Verifica el código y activa la cuenta. Un código incorrecto no revierte la transacción: así el
	 * intento fallido queda contado.
	 *
	 * @param solicitud correo y código
	 * @return mensaje de confirmación
	 * @throws NoEncontradoException si no hay cuenta con ese correo
	 * @throws ReglaNegocioException si el código es incorrecto, venció, ya se usó o se agotaron los intentos
	 */
	@Transactional(noRollbackFor = ReglaNegocioException.class)
	public MensajeResponse verificar(VerificarRequest solicitud) {
		String correoNormal = normalizar(solicitud.getCorreo());
		Usuario usuario = usuarios.findByCorreo(correoNormal).orElseThrow(() -> {
			auditoria.registrarFallido(AccionAuditoria.VERIFICACION_CODIGO, null, correoNormal, ENTIDAD, null,
					"Correo sin cuenta");
			return new NoEncontradoException("No existe una cuenta con ese correo");
		});
		String id = String.valueOf(usuario.getId());

		if (usuario.getEstado() == EstadoCuenta.ACTIVO) {
			auditoria.registrarFallido(AccionAuditoria.VERIFICACION_CODIGO, usuario.getId(), correoNormal, ENTIDAD, id,
					"La cuenta ya está activa");
			throw new ReglaNegocioException(CodigoError.CODIGO_INVALIDO, "El código es incorrecto, venció o ya se usó");
		}

		CodigoVerificacionService.Veredicto veredicto = codigos.validar(usuario, solicitud.getCodigo());
		switch (veredicto.resultado()) {
			case CORRECTO -> {
				usuario.activar();
				auditoria.registrarExitoso(AccionAuditoria.VERIFICACION_CODIGO, usuario.getId(), correoNormal, ENTIDAD,
						id, null);
				return new MensajeResponse("Tu cuenta quedó verificada. Ya puedes iniciar sesión");
			}
			case INCORRECTO -> {
				auditoria.registrarFallido(AccionAuditoria.VERIFICACION_CODIGO, usuario.getId(), correoNormal, ENTIDAD,
						id, "Código incorrecto");
				throw new ReglaNegocioException(CodigoError.CODIGO_INVALIDO,
						"El código es incorrecto. Te quedan " + veredicto.intentosRestantes() + " intentos");
			}
			case AGOTADO -> {
				auditoria.registrarFallido(AccionAuditoria.VERIFICACION_CODIGO, usuario.getId(), correoNormal, ENTIDAD,
						id, "Intentos agotados");
				throw new ReglaNegocioException(CodigoError.CODIGO_INVALIDO,
						"Agotaste los intentos. Solicita un código nuevo");
			}
			default -> {
				auditoria.registrarFallido(AccionAuditoria.VERIFICACION_CODIGO, usuario.getId(), correoNormal, ENTIDAD,
						id, "Código no vigente");
				throw new ReglaNegocioException(CodigoError.CODIGO_INVALIDO,
						"El código venció, ya se usó o fue reemplazado. Solicita uno nuevo");
			}
		}
	}

	/**
	 * Envía un código nuevo a una cuenta pendiente; el anterior queda invalidado (RF-05).
	 *
	 * @param solicitud correo de la cuenta
	 * @return mensaje de confirmación
	 * @throws NoEncontradoException       si no hay cuenta con ese correo
	 * @throws ConflictoException          si la cuenta ya está activa
	 * @throws DemasiadosIntentosException si el último envío fue hace menos de 60 segundos
	 */
	@Transactional
	public MensajeResponse reenviar(ReenviarRequest solicitud) {
		String correoNormal = normalizar(solicitud.getCorreo());
		Usuario usuario = usuarios.findByCorreo(correoNormal).orElseThrow(() -> {
			auditoria.registrarFallido(AccionAuditoria.REENVIO_CODIGO, null, correoNormal, ENTIDAD, null,
					"Correo sin cuenta");
			return new NoEncontradoException("No existe una cuenta con ese correo");
		});
		String id = String.valueOf(usuario.getId());

		if (usuario.getEstado() == EstadoCuenta.ACTIVO) {
			auditoria.registrarFallido(AccionAuditoria.REENVIO_CODIGO, usuario.getId(), correoNormal, ENTIDAD, id,
					"La cuenta ya está activa");
			throw new ConflictoException(CodigoError.CUENTA_YA_ACTIVA, "Esta cuenta ya fue verificada");
		}
		esperarSiEnvioReciente(usuario, AccionAuditoria.REENVIO_CODIGO, correoNormal);

		enviarCodigo(usuario);
		auditoria.registrarExitoso(AccionAuditoria.REENVIO_CODIGO, usuario.getId(), correoNormal, ENTIDAD, id, null);
		return new MensajeResponse(MENSAJE_ENVIADO);
	}

	/**
	 * Genera un código y lo envía; es el último paso de cada operación para poder revertirla si el correo falla.
	 *
	 * @param usuario cuenta destinataria
	 */
	private void enviarCodigo(Usuario usuario) {
		correo.enviarCodigo(usuario.getCorreo(), codigos.generar(usuario));
	}

	/**
	 * Rechaza la operación si el último código se envió hace menos de 60 segundos (propuesta P-02): evita
	 * usar el sistema para llenar de correos la bandeja de otra persona.
	 *
	 * @param usuario cuenta destinataria
	 * @param accion  acción que se anota en la auditoría del rechazo
	 * @param correo  correo de la cuenta
	 * @throws DemasiadosIntentosException si todavía hay que esperar
	 */
	private void esperarSiEnvioReciente(Usuario usuario, AccionAuditoria accion, String correo) {
		Optional<Long> espera = codigos.segundosDeEspera(usuario);
		if (espera.isPresent()) {
			auditoria.registrarFallido(accion, usuario.getId(), correo, ENTIDAD, String.valueOf(usuario.getId()),
					"Envío demasiado reciente");
			throw new DemasiadosIntentosException(
					"Ya enviamos un código hace poco. Espera " + espera.get() + " segundos para pedir otro",
					espera.get());
		}
	}

	/**
	 * Audita un registro rechazado y arma el conflicto.
	 *
	 * @param codigo  código de la familia 409
	 * @param correo  correo intentado
	 * @param mensaje explicación para la persona
	 * @return la excepción lista para lanzar
	 */
	private ConflictoException rechazarRegistro(CodigoError codigo, String correo, String mensaje) {
		auditoria.registrarFallido(AccionAuditoria.REGISTRO_ESTUDIANTE, null, correo, ENTIDAD, null, mensaje);
		return new ConflictoException(codigo, mensaje);
	}

	/**
	 * Indica si la cuenta es un estudiante que aún no verificó el correo.
	 *
	 * @param usuario cuenta a revisar
	 * @return {@code true} si se puede volver a registrar
	 */
	private static boolean esPendiente(Usuario usuario) {
		return usuario.getRol() == Rol.ESTUDIANTE && usuario.getEstado() == EstadoCuenta.PENDIENTE;
	}

	/**
	 * Deja el correo sin espacios y en minúsculas, como se guarda.
	 *
	 * @param correo correo recibido
	 * @return el correo normalizado
	 */
	private static String normalizar(String correo) {
		return correo.trim().toLowerCase(Locale.ROOT);
	}
}
