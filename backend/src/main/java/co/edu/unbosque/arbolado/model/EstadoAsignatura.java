package co.edu.unbosque.arbolado.model;

/** Situación de una asignatura del plan para un estudiante, resumida a partir de todos sus intentos. */
public enum EstadoAsignatura {

	/** Nunca la ha inscrito. */
	POR_CURSAR,

	/** La está cursando en el periodo activo. */
	CURSANDO,

	/** La aprobó en algún intento. */
	APROBADA,

	/** La reprobó y no la está cursando de nuevo. */
	REPROBADA
}
