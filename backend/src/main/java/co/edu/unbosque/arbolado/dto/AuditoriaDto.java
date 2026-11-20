package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Auditoria;
import co.edu.unbosque.arbolado.model.Resultado;

import java.time.Instant;

/**
 * Un registro de auditoría tal como lo ve el administrador.
 *
 * @param id                   identificador
 * @param usuarioId            usuario que originó el evento, o {@code null} si fue anónimo
 * @param usuarioCorreo        correo del usuario o el correo intentado
 * @param accion               evento
 * @param fechaHora            instante del evento, en UTC
 * @param entidad              tipo de recurso afectado
 * @param idRecurso            identificador del recurso afectado
 * @param resultado            exitoso o fallido
 * @param informacionAdicional detalle breve
 */
public record AuditoriaDto(Long id, Long usuarioId, String usuarioCorreo, AccionAuditoria accion, Instant fechaHora,
						   String entidad, String idRecurso, Resultado resultado, String informacionAdicional) {

	/**
	 * Convierte la entidad.
	 *
	 * @param registro registro de auditoría
	 * @return el DTO
	 */
	public static AuditoriaDto de(Auditoria registro) {
		return new AuditoriaDto(registro.getId(), registro.getUsuarioId(), registro.getUsuarioCorreo(),
				registro.getAccion(), registro.getFechaHora(), registro.getEntidad(), registro.getIdRecurso(),
				registro.getResultado(), registro.getInformacionAdicional());
	}
}
