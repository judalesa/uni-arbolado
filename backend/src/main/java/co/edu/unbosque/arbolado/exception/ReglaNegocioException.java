package co.edu.unbosque.arbolado.exception;

import co.edu.unbosque.arbolado.dto.Motivo;

import java.util.List;

/**
 * Una regla de negocio no se cumple (HTTP 422). Puede traer varios {@link Motivo}: las validaciones de
 * inscripción se evalúan todas y se informan juntas.
 */
public class ReglaNegocioException extends ApiException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/** Lista inmutable de incumplimientos; es {@code transient} porque {@link Motivo} no se serializa. */
	private final transient List<Motivo> motivos;

	/**
	 * Regla genérica ({@link CodigoError#REGLA_NEGOCIO}) con la lista completa de motivos.
	 *
	 * @param mensaje explicación general
	 * @param motivos todos los incumplimientos; se copia la lista
	 */
	public ReglaNegocioException(String mensaje, List<Motivo> motivos) {
		this(CodigoError.REGLA_NEGOCIO, mensaje, motivos);
	}

	/**
	 * Regla con un código específico de la familia 422 y sin motivos (por ejemplo un código de
	 * verificación inválido).
	 *
	 * @param codigo  código de la familia 422
	 * @param mensaje explicación legible
	 */
	public ReglaNegocioException(CodigoError codigo, String mensaje) {
		this(codigo, mensaje, List.of());
	}

	/**
	 * Constructor completo.
	 *
	 * @param codigo  código de la familia 422
	 * @param mensaje explicación general
	 * @param motivos incumplimientos (puede estar vacía); se copia la lista
	 * @throws IllegalArgumentException si el código no es de la familia 422
	 */
	public ReglaNegocioException(CodigoError codigo, String mensaje, List<Motivo> motivos) {
		super(codigo, 422, mensaje);
		this.motivos = List.copyOf(motivos);
	}

	/**
	 * Motivos del rechazo.
	 *
	 * @return lista inmutable, vacía si no hay motivos
	 */
	public List<Motivo> getMotivos() {
		return motivos;
	}
}
