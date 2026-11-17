package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Auditoria;
import co.edu.unbosque.arbolado.model.Resultado;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Escribe la auditoría (RN-30, RN-31). Los eventos exitosos van en la transacción de la operación; los
 * fallidos, en una transacción independiente para que sobrevivan a la reversión.
 */
@Service
public class AuditoriaService {

	/** Longitud máxima de la información adicional. */
	static final int MAX_INFORMACION = 1000;

	/** Repositorio de auditoría. */
	private final AuditoriaRepository repositorio;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio repositorio de auditoría
	 */
	public AuditoriaService(AuditoriaRepository repositorio) {
		this.repositorio = repositorio;
	}

	/**
	 * Registra un evento exitoso dentro de la transacción de quien lo llama.
	 *
	 * @param accion        evento ocurrido
	 * @param usuarioId     usuario que lo originó, o {@code null} si es anónimo
	 * @param usuarioCorreo correo del usuario o el correo intentado
	 * @param entidad       tipo de recurso afectado
	 * @param idRecurso     identificador del recurso afectado
	 * @param informacion   detalle breve, sin contraseñas ni códigos
	 * @return identificador del registro creado
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public Long registrarExitoso(AccionAuditoria accion, Long usuarioId, String usuarioCorreo,
								 String entidad, String idRecurso, String informacion) {
		return guardar(accion, usuarioId, usuarioCorreo, entidad, idRecurso, Resultado.EXITOSO, informacion);
	}

	/**
	 * Registra un evento fallido en una transacción nueva e independiente, para que sobreviva aunque la
	 * transacción de quien lo llama se revierta.
	 *
	 * @param accion        evento rechazado
	 * @param usuarioId     usuario que lo originó, o {@code null} si es anónimo
	 * @param usuarioCorreo correo del usuario o el correo intentado
	 * @param entidad       tipo de recurso afectado
	 * @param idRecurso     identificador del recurso afectado
	 * @param informacion   motivo breve, sin contraseñas ni códigos
	 * @return identificador del registro creado
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long registrarFallido(AccionAuditoria accion, Long usuarioId, String usuarioCorreo,
								 String entidad, String idRecurso, String informacion) {
		return guardar(accion, usuarioId, usuarioCorreo, entidad, idRecurso, Resultado.FALLIDO, informacion);
	}

	/**
	 * Construye y guarda el registro, recortando la información adicional a su máximo.
	 *
	 * @param accion        evento
	 * @param usuarioId     usuario o {@code null}
	 * @param usuarioCorreo correo del usuario o el intentado
	 * @param entidad       tipo de recurso
	 * @param idRecurso     identificador del recurso
	 * @param resultado     resultado del evento
	 * @param informacion   detalle breve
	 * @return identificador del registro creado
	 */
	private Long guardar(AccionAuditoria accion, Long usuarioId, String usuarioCorreo, String entidad,
						 String idRecurso, Resultado resultado, String informacion) {
		Objects.requireNonNull(accion, "La acción es obligatoria");
		String detalle = informacion != null && informacion.length() > MAX_INFORMACION
				? informacion.substring(0, MAX_INFORMACION) : informacion;
		return repositorio.save(new Auditoria(accion, usuarioId, usuarioCorreo, entidad, idRecurso, resultado, detalle))
				.getId();
	}
}
