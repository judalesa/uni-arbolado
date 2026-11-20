package co.edu.unbosque.arbolado.exception;

/**
 * Catálogo de motivos por los que se puede rechazar una inscripción (campo {@code motivos[].codigo}
 * del contrato). Se devuelven todos los que apliquen, no solo el primero.
 */
public enum CodigoMotivo {
	/** Falta aprobar un prerrequisito; "Cursando" no cuenta. */
	PREREQUISITO_NO_CUMPLIDO,
	/** Superaría el máximo de 12 créditos por periodo. */
	CREDITOS_EXCEDIDOS,
	/** El grupo ya tiene 30 estudiantes cursando. */
	GRUPO_LLENO,
	/** La asignatura ya fue aprobada. */
	ASIGNATURA_YA_APROBADA,
	/** La asignatura ya está cursándose en otro grupo. */
	ASIGNATURA_YA_CURSANDO,
	/** El horario se cruza con otro grupo que el estudiante cursa. */
	HORARIO_TRASLAPADO,
	/** No hay un periodo activo en el cual inscribirse. */
	PERIODO_NO_ACTIVO
}
