package co.edu.unbosque.arbolado.exception;

/**
 * El servidor de correo no aceptó el mensaje. No es una excepción de negocio: se responde 500 genérico y
 * el detalle queda en el registro.
 */
public class CorreoNoEnviadoException extends RuntimeException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción.
	 *
	 * @param mensaje explicación técnica para el registro
	 * @param causa   error original del servidor de correo
	 */
	public CorreoNoEnviadoException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}
}
