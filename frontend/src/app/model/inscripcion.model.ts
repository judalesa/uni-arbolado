import { GrupoDto } from './grupo.model';
import { UsuarioResumen } from './usuario.model';

/** Estado de una inscripción: nace cursando y termina aprobada o reprobada con la definitiva. */
export type EstadoInscripcion = 'CURSANDO' | 'APROBADA' | 'REPROBADA';

/** Cuerpo de `POST /api/inscripciones` (`InscripcionRequest`); el estudiante sale del token. */
export interface InscripcionRequest {
  /** Grupo en el que se inscribe. */
  grupoId: number;
}

/** Corte de notas: 1 y 2 pesan 30 % y el 3 pesa 40 %. */
export type Corte = 1 | 2 | 3;

/** Cuerpo de `POST /api/profesor/inscripciones/{id}/notas` (`NotaRequest`). */
export interface NotaRequest {
  /** Corte al que corresponde la nota. */
  corte: Corte;
  /** Nota de 0.0 a 5.0 con un decimal. */
  valor: number;
}

/** Inscripción con sus notas (`InscripcionDto`). */
export interface InscripcionDto {
  /** Identificador. */
  id: number;
  /** Estudiante inscrito; lo incluyen las consultas del profesor. */
  estudiante?: UsuarioResumen;
  /** Grupo de la inscripción. */
  grupo: GrupoDto;
  /** Estado actual. */
  estado: EstadoInscripcion;
  /** Nota del primer corte (30 %), o `null` si no se ha registrado. */
  notaCorte1?: number | null;
  /** Nota del segundo corte (30 %), o `null`. */
  notaCorte2?: number | null;
  /** Nota del tercer corte (40 %), o `null`. */
  notaCorte3?: number | null;
  /** Definitiva calculada por el backend, redondeada a un decimal; `null` hasta tener los tres cortes. */
  notaDefinitiva?: number | null;
  /** Instante de la inscripción (UTC). */
  fechaInscripcion?: string;
}
