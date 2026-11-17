package co.edu.unbosque.arbolado.exception;

/**
 * Catálogo de códigos de error estables de la API, cada uno con su estado HTTP estándar.
 *
 * <p><b>Por qué códigos propios además del HTTP:</b> con solo el número, el frontend no distingue
 * "documento duplicado" de "correo duplicado" (ambos 409). El código textual sí, y no cambia aunque
 * cambie el mensaje. La tabla coincide con la del contrato {@code openapi.yaml}.</p>
 */
public enum CodigoError {

	/** Datos de entrada inválidos. */
	VALIDACION(400),

	/** Sin token, o token inválido, vencido o revocado. */
	NO_AUTENTICADO(401),
	/** Correo o contraseña incorrectos. */
	CREDENCIALES_INVALIDAS(401),

	/** El rol no alcanza o el recurso pertenece a otra persona. */
	ACCESO_DENEGADO(403),
	/** La cuenta existe pero aún no se ha verificado el correo. */
	CUENTA_PENDIENTE(403),

	/** El recurso solicitado no existe. */
	NO_ENCONTRADO(404),
	/** Método HTTP no permitido en esa ruta. */
	METODO_NO_PERMITIDO(405),
	/** La API solo responde JSON. */
	NO_ACEPTABLE(406),

	/** Ya existe una cuenta con ese documento. */
	DOCUMENTO_DUPLICADO(409),
	/** Ya existe una cuenta con ese correo. */
	CORREO_DUPLICADO(409),
	/** La cuenta ya fue verificada. */
	CUENTA_YA_ACTIVA(409),
	/** Ya existe un periodo con ese nombre. */
	PERIODO_DUPLICADO(409),
	/** El corte ya tiene nota (las notas no se modifican, decisión A-06). */
	NOTA_YA_REGISTRADA(409),
	/** Ya existe un grupo con ese número en la asignatura y el periodo. */
	GRUPO_DUPLICADO(409),

	/** El cuerpo no es {@code application/json}. */
	TIPO_NO_SOPORTADO(415),

	/** Regla de negocio incumplida; trae la lista de motivos (RN-22). */
	REGLA_NEGOCIO(422),
	/** Código de verificación incorrecto, vencido o ya usado. */
	CODIGO_INVALIDO(422),
	/** No se puede activar otro periodo mientras haya notas pendientes (decisión A-02). */
	PERIODO_CON_NOTAS_PENDIENTES(422),
	/** La operación exige un periodo activo y no lo hay. */
	PERIODO_NO_ACTIVO(422),

	/** Demasiados reenvíos de código; hay que esperar. */
	DEMASIADOS_INTENTOS(429),

	/** Error inesperado: el detalle solo queda en el registro del servidor. */
	ERROR_INTERNO(500);

	/** Estado HTTP asociado al código. */
	private final int estado;

	/**
	 * Asocia el código con su estado HTTP.
	 *
	 * @param estado número de estado HTTP, por ejemplo 409
	 */
	CodigoError(int estado) {
		this.estado = estado;
	}

	/**
	 * Estado HTTP asociado al código.
	 *
	 * @return el número de estado, por ejemplo 409
	 */
	public int estado() {
		return estado;
	}
}
