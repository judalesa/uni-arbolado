package co.edu.unbosque.arbolado.exception;

/**
 * Catálogo de motivos por los que se puede rechazar una inscripción (campo {@code motivos[].codigo}
 * del contrato). Se devuelven todos los que apliquen, no solo el primero (RN-22).
 */
public enum CodigoMotivo {
	/** Falta aprobar un prerrequisito; "Cursando" no cuenta (RN-16). */
	PREREQUISITO_NO_CUMPLIDO,
	/** Superaría el máximo de 12 créditos por periodo (RN-17). */
	CREDITOS_EXCEDIDOS,
	/** El grupo ya tiene 30 estudiantes cursando (RN-11, RN-18). */
	GRUPO_LLENO,
	/** La asignatura ya fue aprobada (RN-19). */
	ASIGNATURA_YA_APROBADA,
	/** La asignatura ya está cursándose en otro grupo (RN-19). */
	ASIGNATURA_YA_CURSANDO,
	/** El horario se cruza con otro grupo que el estudiante cursa (RN-21). */
	HORARIO_TRASLAPADO,
	/** No hay un periodo activo en el cual inscribirse (RN-14). */
	PERIODO_NO_ACTIVO
}
