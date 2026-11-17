package co.edu.unbosque.arbolado.exception;

/**
 * Los datos chocan con algo que ya existe, por ejemplo un documento o correo repetido (HTTP 409).
 */
public class ConflictoException extends ApiException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción de conflicto.
	 *
	 * @param codigo  código de la familia 409 (por ejemplo {@link CodigoError#DOCUMENTO_DUPLICADO})
	 * @param mensaje explicación legible
	 * @throws IllegalArgumentException si el código no es de la familia 409
	 */
	public ConflictoException(CodigoError codigo, String mensaje) {
		super(codigo, 409, mensaje);
	}
}
