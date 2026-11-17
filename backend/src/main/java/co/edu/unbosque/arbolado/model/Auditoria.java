package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Registro de un evento auditable. Es inmutable: no tiene setters, Hibernate no lo actualiza
 * ({@link Immutable}) y ninguna columna es actualizable. No tiene llave foránea a {@code usuario}.
 */
@Entity
@Immutable
@Table(name = "auditoria")
public class Auditoria {

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Identificador del usuario; es {@code null} en eventos anónimos. Sin llave foránea a propósito. */
	@Column(name = "usuario_id", updatable = false)
	private Long usuarioId;

	/** Correo del usuario en el momento del evento; en eventos anónimos, el correo intentado. */
	@Column(name = "usuario_correo", updatable = false, length = 120)
	private String usuarioCorreo;

	/** Evento ocurrido. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 40)
	private AccionAuditoria accion;

	/** Instante del evento en UTC. */
	@Column(name = "fecha_hora", nullable = false, updatable = false)
	private Instant fechaHora;

	/** Tipo de recurso afectado, por ejemplo {@code INSCRIPCION}. */
	@Column(updatable = false, length = 40)
	private String entidad;

	/** Identificador del recurso afectado, como texto. */
	@Column(name = "id_recurso", updatable = false, length = 60)
	private String idRecurso;

	/** Resultado del evento. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 20)
	private Resultado resultado;

	/** Detalle breve. Nunca contiene contraseñas ni códigos. */
	@Column(name = "informacion_adicional", updatable = false, length = 1000)
	private String informacionAdicional;

	/** Constructor sin argumentos que exige JPA. */
	protected Auditoria() {
	}

	/**
	 * Crea un registro con la hora actual. El identificador lo asigna la base de datos.
	 *
	 * @param accion               evento ocurrido
	 * @param usuarioId            usuario que lo originó, o {@code null} si es anónimo
	 * @param usuarioCorreo        correo del usuario o el correo intentado
	 * @param entidad              tipo de recurso afectado
	 * @param idRecurso            identificador del recurso afectado
	 * @param resultado            resultado del evento
	 * @param informacionAdicional detalle breve, sin datos sensibles
	 */
	public Auditoria(AccionAuditoria accion, Long usuarioId, String usuarioCorreo, String entidad,
					 String idRecurso, Resultado resultado, String informacionAdicional) {
		this.accion = accion;
		this.usuarioId = usuarioId;
		this.usuarioCorreo = usuarioCorreo;
		this.entidad = entidad;
		this.idRecurso = idRecurso;
		this.resultado = resultado;
		this.informacionAdicional = informacionAdicional;
		this.fechaHora = Instant.now();
	}

	/** @return identificador del registro */
	public Long getId() {
		return id;
	}

	/** @return identificador del usuario, o {@code null} */
	public Long getUsuarioId() {
		return usuarioId;
	}

	/** @return correo del usuario o el intentado */
	public String getUsuarioCorreo() {
		return usuarioCorreo;
	}

	/** @return evento ocurrido */
	public AccionAuditoria getAccion() {
		return accion;
	}

	/** @return instante del evento (UTC) */
	public Instant getFechaHora() {
		return fechaHora;
	}

	/** @return tipo de recurso afectado */
	public String getEntidad() {
		return entidad;
	}

	/** @return identificador del recurso afectado */
	public String getIdRecurso() {
		return idRecurso;
	}

	/** @return resultado del evento */
	public Resultado getResultado() {
		return resultado;
	}

	/** @return detalle breve */
	public String getInformacionAdicional() {
		return informacionAdicional;
	}
}
