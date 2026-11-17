package co.edu.unbosque.arbolado.exception;

/**
 * Se pidió una acción restringida por tiempo demasiadas veces, por ejemplo reenviar el código de
 * verificación (HTTP 429). Indica cuántos segundos hay que esperar.
 */
public class DemasiadosIntentosException extends ApiException {

	/** Versión de serialización: las excepciones son {@link java.io.Serializable} por herencia de {@link Throwable}. */
	private static final long serialVersionUID = 1L;

	/** Segundos que la persona debe esperar antes de volver a intentar. */
	private final long segundosEspera;

	/**
	 * Crea la excepción.
	 *
	 * @param mensaje        explicación legible
	 * @param segundosEspera tiempo que debe esperar la persona; se envía en la cabecera {@code Retry-After}
	 */
	public DemasiadosIntentosException(String mensaje, long segundosEspera) {
		super(CodigoError.DEMASIADOS_INTENTOS, 429, mensaje);
		this.segundosEspera = segundosEspera;
	}

	/**
	 * Segundos que debe esperar la persona antes de reintentar.
	 *
	 * @return la espera en segundos
	 */
	public long getSegundosEspera() {
		return segundosEspera;
	}
}
