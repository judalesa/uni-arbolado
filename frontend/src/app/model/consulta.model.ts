import { AsignaturaDto, AsignaturaResumen } from './asignatura.model';
import { DiaSemana } from './grupo.model';
import { InscripcionDto } from './inscripcion.model';
import { PeriodoDto } from './periodo.model';

/** Estado de una asignatura del plan para un estudiante. */
export type EstadoAsignatura = 'POR_CURSAR' | 'CURSANDO' | 'APROBADA' | 'REPROBADA';

/** Asignatura del plan con su estado para el estudiante (`AsignaturaEstadoDto`). */
export interface AsignaturaEstadoDto {
  /** Asignatura del plan. */
  asignatura: AsignaturaDto;
  /** Estado para el estudiante. */
  estado: EstadoAsignatura;
  /** Definitiva del intento que define el estado, o `null`. */
  notaDefinitiva?: number | null;
}

/** Una franja del horario semanal (`ClaseDto`). */
export interface ClaseDto {
  /** Asignatura de la clase. */
  asignatura: AsignaturaResumen;
  /** Grupo de la clase. */
  grupoId?: number;
  /** Número del grupo. */
  grupoNumero: number;
  /** Bloque del salón. */
  bloque?: string;
  /** Salón. */
  salon?: string;
  /** Día de la semana. */
  diaSemana: DiaSemana;
  /** Hora de inicio `HH:mm`. */
  horaInicio: string;
  /** Hora de fin `HH:mm`. */
  horaFin: string;
}

/** Inscripciones de un periodo dentro del historial (`HistorialPeriodoDto`). */
export interface HistorialPeriodoDto {
  /** Periodo. */
  periodo: PeriodoDto;
  /** Intentos del periodo con sus notas. */
  inscripciones: InscripcionDto[];
}
