import { AsignaturaResumen } from './asignatura.model';
import { PeriodoDto } from './periodo.model';
import { UsuarioResumen } from './usuario.model';

/** Días en que hay clase: de lunes a sábado. */
export type DiaSemana = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES' | 'SABADO';

/** Franja semanal de un grupo (`HorarioDto`); las horas son locales, sin zona. */
export interface HorarioDto {
  /** Día de la semana. */
  diaSemana: DiaSemana;
  /** Hora de inicio con formato `HH:mm`. */
  horaInicio: string;
  /** Hora de fin con formato `HH:mm`. */
  horaFin: string;
}

/** Cuerpo para crear o modificar un grupo (`GrupoRequest`). */
export interface GrupoRequest {
  /** Asignatura que se dicta. */
  asignaturaId: number;
  /** Periodo del grupo. */
  periodoId: number;
  /** Profesor asignado. */
  profesorId: number;
  /** Bloque del salón. */
  bloque: string;
  /** Salón. */
  salon: string;
  /** Franjas semanales; al menos una. */
  horarios: HorarioDto[];
}

/** Grupo con sus cupos y horarios (`GrupoDto`). */
export interface GrupoDto {
  /** Identificador. */
  id: number;
  /** Número del grupo dentro de la asignatura y el periodo. */
  numero: number;
  /** Asignatura que se dicta. */
  asignatura: AsignaturaResumen;
  /** Periodo del grupo. */
  periodo: PeriodoDto;
  /** Profesor asignado. */
  profesor: UsuarioResumen;
  /** Bloque del salón. */
  bloque: string;
  /** Salón. */
  salon: string;
  /** Cupo máximo (siempre 30). */
  cupoMaximo: number;
  /** Estudiantes cursando. */
  inscritos: number;
  /** Cupos libres. */
  cuposDisponibles: number;
  /** Franjas semanales. */
  horarios: HorarioDto[];
}
