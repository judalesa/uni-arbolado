package co.edu.unbosque.arbolado.exception;

/** El recurso solicitado no existe (HTTP 404, {@link CodigoError#NO_ENCONTRADO}). */
public class NoEncontradoException extends ApiException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con un mensaje propio.
	 *
	 * @param mensaje explicación legible
	 */
	public NoEncontradoException(String mensaje) {
		super(CodigoError.NO_ENCONTRADO, 404, mensaje);
	}

	/**
	 * Crea la excepción con un mensaje estándar a partir del tipo de recurso y su identificador.
	 *
	 * @param recurso nombre legible del recurso, por ejemplo "La asignatura"
	 * @param id      identificador buscado
	 * @return la excepción lista para lanzar
	 */
	public static NoEncontradoException de(String recurso, Object id) {
		return new NoEncontradoException(recurso + " con identificador " + id + " no existe");
	}
}
