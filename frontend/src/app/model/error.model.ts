/** Error de validación de un campo (solo en respuestas 400). */
export interface DetalleCampo {
  /** Nombre del campo. */
  campo: string;
  /** Mensaje del error. */
  mensaje: string;
}

/** Motivo de incumplimiento de una regla (solo en respuestas 422). */
export interface Motivo {
  /** Código estable del motivo. */
  codigo: string;
  /** Mensaje para el usuario. */
  mensaje: string;
}

/** Formato de error de toda la API (`ErrorResponse`). */
export interface ErrorResponse {
  /** Instante del error (UTC). */
  timestamp: string;
  /** Código HTTP. */
  status: number;
  /** Código estable; el frontend decide con él, nunca lo muestra. */
  codigo: string;
  /** Mensaje en español. */
  mensaje: string;
  /** Ruta que falló. */
  ruta: string;
  /** Errores por campo (400). */
  detalles?: DetalleCampo[];
  /** Todos los incumplimientos de la regla (422). */
  motivos?: Motivo[];
}
