package co.edu.unbosque.arbolado.exception;

/**
 * La persona está autenticada pero no puede hacer esto: rol insuficiente, recurso de otra persona o
 * cuenta pendiente de verificación (HTTP 403).
 */
public class AccesoDenegadoException extends ApiException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción de acceso denegado.
	 *
	 * @param codigo  {@link CodigoError#ACCESO_DENEGADO} o {@link CodigoError#CUENTA_PENDIENTE}
	 * @param mensaje explicación legible
	 * @throws IllegalArgumentException si el código no es de la familia 403
	 */
	public AccesoDenegadoException(CodigoError codigo, String mensaje) {
		super(codigo, 403, mensaje);
	}
}
