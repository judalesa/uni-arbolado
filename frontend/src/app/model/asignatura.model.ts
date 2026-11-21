/** Datos mínimos de una asignatura dentro de otro objeto (`AsignaturaResumen`). */
export interface AsignaturaResumen {
  /** Identificador. */
  id: number;
  /** Código, por ejemplo `DSI301`. */
  codigo: string;
  /** Nombre. */
  nombre: string;
  /** Créditos. */
  creditos: number;
}

/** Asignatura del plan de estudios con sus prerrequisitos (`AsignaturaDto`). */
export interface AsignaturaDto {
  /** Identificador. */
  id: number;
  /** Código. */
  codigo: string;
  /** Nombre. */
  nombre: string;
  /** Descripción breve. */
  descripcion?: string;
  /** Semestre del plan, de 1 a 9. */
  semestre: number;
  /** Créditos, de 1 a 5. */
  creditos: number;
  /** Asignaturas que deben estar aprobadas antes. */
  prerrequisitos: AsignaturaResumen[];
}
