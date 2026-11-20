package co.edu.unbosque.arbolado.model;

/**
 * Estado de una inscripción. "Por cursar" no se guarda: se deduce de que el estudiante no tenga
 * ninguna inscripción vigente de la asignatura.
 */
public enum EstadoInscripcion {
	/** En curso; ocupa cupo y cuenta créditos. */
	CURSANDO,
	/** Terminada con definitiva de 3.0 o más. */
	APROBADA,
	/** Terminada con definitiva menor de 3.0; se puede reinscribir en un periodo posterior. */
	REPROBADA
}
