import { ColumnaTabla } from '../component/tabla/tabla.component';
import { InscripcionDto } from '../model/inscripcion.model';

/**
 * Muestra una nota siempre con un decimal (`3.0`, no `3`). La nota ya llega redondeada del backend;
 * aquí solo se formatea.
 *
 * @param nota nota de 0.0 a 5.0; `null` u omitida si aún no se registra
 * @return nota con un decimal, o guion si no hay nota
 */
export const formatearNota = (nota?: number | null): string =>
  nota === null || nota === undefined ? '—' : nota.toFixed(1);

/** Nota mínima. */
export const NOTA_MINIMA = 0;

/** Nota máxima. */
export const NOTA_MAXIMA = 5;

/**
 * Convierte el texto escrito por el profesor en una nota válida. Acepta coma o punto decimal. Es una
 * ayuda para no enviar valores errados; el backend vuelve a validar.
 *
 * @param texto texto del campo
 * @return la nota, o `null` si no es un número de 0.0 a 5.0 con máximo un decimal
 */
export const leerNota = (texto: string): number | null => {
  const limpio = texto.trim().replace(',', '.');
  if (!/^\d(\.\d)?$/.test(limpio)) {
    return null;
  }
  const valor = Number(limpio);
  return valor >= NOTA_MINIMA && valor <= NOTA_MAXIMA ? valor : null;
};

/**
 * Columnas de una tabla de inscripciones con sus notas: asignatura, cortes y definitiva. Las comparten
 * las consultas del estudiante y del profesor.
 *
 * @return columnas de la tabla
 */
export const columnasDeNotas = (): ColumnaTabla<InscripcionDto>[] => [
  {
    titulo: 'Asignatura',
    valor: (i) => `${i.grupo.asignatura.codigo} · ${i.grupo.asignatura.nombre}`,
  },
  { titulo: 'Créditos', valor: (i) => i.grupo.asignatura.creditos, clase: 'text-center' },
  { titulo: 'Grupo', valor: (i) => i.grupo.numero, clase: 'text-center' },
  { titulo: 'Corte 1 (30 %)', valor: (i) => formatearNota(i.notaCorte1), clase: 'text-center' },
  { titulo: 'Corte 2 (30 %)', valor: (i) => formatearNota(i.notaCorte2), clase: 'text-center' },
  { titulo: 'Corte 3 (40 %)', valor: (i) => formatearNota(i.notaCorte3), clase: 'text-center' },
  {
    titulo: 'Definitiva',
    valor: (i) => formatearNota(i.notaDefinitiva),
    clase: 'text-center fw-semibold',
  },
];
