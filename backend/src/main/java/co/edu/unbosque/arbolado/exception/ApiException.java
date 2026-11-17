package co.edu.unbosque.arbolado.exception;

/**
 * Base de las excepciones de negocio de la API. Cada una lleva un {@link CodigoError} estable, del que
 * el {@link GlobalExceptionHandler} deduce el estado HTTP y el cuerpo de la respuesta.
 */
public abstract class ApiException extends RuntimeException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/** Código estable del error; de él se deducen el estado HTTP y el texto del campo {@code codigo}. */
	private final CodigoError codigo;

	/**
	 * Crea la excepción comprobando que el código pertenezca a la familia HTTP de la subclase.
	 *
	 * @param codigo          código estable del error
	 * @param estadoEsperado  estado HTTP que debe tener el código (la subclase lo fija)
	 * @param mensaje         explicación legible para el usuario
	 * @throws IllegalArgumentException si el código no corresponde al estado esperado
	 */
	protected ApiException(CodigoError codigo, int estadoEsperado, String mensaje) {
		super(mensaje);
		if (codigo.estado() != estadoEsperado) {
			throw new IllegalArgumentException(
					"El código " + codigo + " es HTTP " + codigo.estado() + ", se esperaba " + estadoEsperado);
		}
		this.codigo = codigo;
	}

	/**
	 * Código estable del error.
	 *
	 * @return el código, por ejemplo {@link CodigoError#CORREO_DUPLICADO}
	 */
	public CodigoError getCodigo() {
		return codigo;
	}
}
