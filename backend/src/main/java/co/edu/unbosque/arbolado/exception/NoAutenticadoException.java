package co.edu.unbosque.arbolado.exception;

/**
 * La persona no está autenticada: credenciales incorrectas, token inválido, vencido o revocado (HTTP 401).
 */
public class NoAutenticadoException extends ApiException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción de autenticación.
	 *
	 * @param codigo  {@link CodigoError#NO_AUTENTICADO} o {@link CodigoError#CREDENCIALES_INVALIDAS}
	 * @param mensaje explicación legible; no debe revelar si falló el correo o la contraseña
	 * @throws IllegalArgumentException si el código no es de la familia 401
	 */
	public NoAutenticadoException(CodigoError codigo, String mensaje) {
		super(codigo, 401, mensaje);
	}
}
