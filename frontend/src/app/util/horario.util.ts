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
 * Error propio de una franja: incompleta o con la hora de fin antes de la de inicio.
 *
 * @param franja franja del formulario
 * @param numero posición visible de la franja, desde 1
 * @return mensaje de error, o `null` si la franja está bien
 */
const errorDeFranja = (franja: FranjaEditable, numero: number): string | null => {
  if (!franja.diaSemana || !franja.horaInicio || !franja.horaFin) {
    return `Completa el día y las horas del horario ${numero}.`;
  }
  return franja.horaFin <= franja.horaInicio
    ? `En el horario ${numero} la hora de fin debe ser posterior a la de inicio.`
    : null;
};

/**
 * Indica si dos franjas completas caen el mismo día y se cruzan; si solo se tocan, no se traslapan.
 *
 * @param primera una franja
 * @param segunda otra franja
 * @return {@code true} si se traslapan
 */
const seTraslapan = (primera: FranjaEditable, segunda: FranjaEditable): boolean =>
  primera.diaSemana === segunda.diaSemana &&
  primera.horaInicio < segunda.horaFin &&
  segunda.horaInicio < primera.horaFin;

/**
 * Mensajes de cada par de franjas que se traslapan.
 *
 * @param franjas franjas completas y bien ordenadas en el tiempo
 * @return un mensaje por cada par traslapado
 */
const traslapes = (franjas: FranjaEditable[]): string[] =>
  franjas.flatMap((primera, indice) =>
    franjas
      .slice(indice + 1)
      .map((segunda, desplazamiento) => ({ segunda, otro: indice + desplazamiento + 2 }))
      .filter(({ segunda }) => seTraslapan(primera, segunda))
      .map(({ otro }) => {
        const dia = DIAS[posicion(primera.diaSemana as DiaSemana)].nombre.toLowerCase();
        return `Los horarios ${indice + 1} y ${otro} se traslapan el ${dia}.`;
      }),
  );

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
  const errores = franjas
    .map((franja, indice) => errorDeFranja(franja, indice + 1))
    .filter((mensaje): mensaje is string => mensaje !== null);
  return errores.length > 0 ? errores : traslapes(franjas);
};
