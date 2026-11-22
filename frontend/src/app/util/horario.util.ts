import { DiaSemana, HorarioDto } from '../model/grupo.model';

/** Día de la semana con sus nombres visibles. */
export interface DiaVista {
  /** Valor del contrato. */
  valor: DiaSemana;
  /** Nombre completo. */
  nombre: string;
  /** Abreviatura para listados. */
  corto: string;
}

/** Días de clase en orden, de lunes a sábado. */
export const DIAS: DiaVista[] = [
  { valor: 'LUNES', nombre: 'Lunes', corto: 'Lu' },
  { valor: 'MARTES', nombre: 'Martes', corto: 'Ma' },
  { valor: 'MIERCOLES', nombre: 'Miércoles', corto: 'Mi' },
  { valor: 'JUEVES', nombre: 'Jueves', corto: 'Ju' },
  { valor: 'VIERNES', nombre: 'Viernes', corto: 'Vi' },
  { valor: 'SABADO', nombre: 'Sábado', corto: 'Sá' },
];

/**
 * Posición de un día en la semana, para ordenar.
 *
 * @param dia día del contrato
 * @return índice de 0 (lunes) a 5 (sábado)
 */
const posicion = (dia: DiaSemana): number => DIAS.findIndex((d) => d.valor === dia);

/**
 * Ordena franjas por día y hora de inicio, sin modificar la lista original.
 *
 * @param horarios franjas a ordenar
 * @return nueva lista ordenada
 */
export const ordenarHorarios = <T extends HorarioDto>(horarios: T[]): T[] =>
  [...horarios].sort(
    (a, b) => posicion(a.diaSemana) - posicion(b.diaSemana) || a.horaInicio.localeCompare(b.horaInicio),
  );

/**
 * Resume las franjas de un grupo en una línea, por ejemplo `Lu 08:00–10:00 · Mi 08:00–10:00`.
 *
 * @param horarios franjas del grupo
 * @return texto del horario
 */
export const formatearHorarios = (horarios: HorarioDto[]): string =>
  ordenarHorarios(horarios)
    .map((h) => `${DIAS[posicion(h.diaSemana)]?.corto ?? h.diaSemana} ${h.horaInicio}–${h.horaFin}`)
    .join(' · ');

/** Franja tal como está en el formulario: puede estar incompleta. */
export interface FranjaEditable {
  /** Día elegido, o vacío. */
  diaSemana: DiaSemana | '';
  /** Hora de inicio, o vacía. */
  horaInicio: string;
  /** Hora de fin, o vacía. */
  horaFin: string;
}

/**
 * Revisa las franjas antes de enviarlas: que haya al menos una, que estén completas, que terminen
 * después de empezar y que no se traslapen. El backend aplica las mismas reglas; revisarlas aquí
 * evita una ida al servidor y señala la fila exacta. Las horas `HH:mm` se comparan como texto.
 *
 * @param franjas franjas del formulario
 * @return mensajes de error; vacío si todo está bien
 */
export const validarHorarios = (franjas: FranjaEditable[]): string[] => {
  if (franjas.length === 0) {
    return ['Agrega al menos un horario.'];
  }
  const errores: string[] = [];
  franjas.forEach((franja, i) => {
    if (!franja.diaSemana || !franja.horaInicio || !franja.horaFin) {
      errores.push(`Completa el día y las horas del horario ${i + 1}.`);
    } else if (franja.horaFin <= franja.horaInicio) {
      errores.push(`En el horario ${i + 1} la hora de fin debe ser posterior a la de inicio.`);
    }
  });
  if (errores.length > 0) {
    return errores;
  }
  for (let i = 0; i < franjas.length; i++) {
    for (let j = i + 1; j < franjas.length; j++) {
      const a = franjas[i];
      const b = franjas[j];
      if (a.diaSemana === b.diaSemana && a.horaInicio < b.horaFin && b.horaInicio < a.horaFin) {
        const dia = DIAS[posicion(a.diaSemana as DiaSemana)].nombre.toLowerCase();
        errores.push(`Los horarios ${i + 1} y ${j + 1} se traslapan el ${dia}.`);
      }
    }
  }
  return errores;
};
