/** Periodo académico (`PeriodoDto`). */
export interface PeriodoDto {
  /** Identificador. */
  id: number;
  /** Código con formato `AAAA-N`, por ejemplo `2026-2`. */
  codigo: string;
  /** Fecha de inicio (`AAAA-MM-DD`). */
  fechaInicio?: string;
  /** Fecha de fin (`AAAA-MM-DD`). */
  fechaFin?: string;
  /** Si es el periodo activo. */
  activo: boolean;
}

/** Cuerpo para crear o modificar un periodo (`PeriodoRequest`). */
export interface PeriodoRequest {
  /** Código con formato `AAAA-N`. */
  codigo: string;
  /** Fecha de inicio (`AAAA-MM-DD`). */
  fechaInicio: string;
  /** Fecha de fin (`AAAA-MM-DD`), posterior a la de inicio. */
  fechaFin: string;
}
