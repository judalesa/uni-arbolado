import { GrupoDto } from '../model/grupo.model';
import { PeriodoDto } from '../model/periodo.model';

/** Periodos de ejemplo para las pruebas: el 2026-2 está activo. */
export const PERIODOS_PRUEBA: PeriodoDto[] = [
  { id: 1, codigo: '2026-1', fechaInicio: '2026-01-19', fechaFin: '2026-06-06', activo: false },
  { id: 2, codigo: '2026-2', fechaInicio: '2026-07-20', fechaFin: '2026-12-05', activo: true },
];

/**
 * Crea un grupo de ejemplo para las pruebas.
 *
 * @param cambios campos que se quieren distintos al valor por defecto
 * @return grupo de ejemplo
 */
export const grupoDePrueba = (cambios: Partial<GrupoDto> = {}): GrupoDto => ({
  id: 10,
  numero: 1,
  asignatura: { id: 4, codigo: 'DSI301', nombre: 'Desarrollo de Sistemas 3', creditos: 4 },
  periodo: PERIODOS_PRUEBA[1],
  profesor: { id: 5, documento: '1012345678', nombreCompleto: 'Marta Rojas' },
  bloque: 'B',
  salon: '301',
  cupoMaximo: 30,
  inscritos: 0,
  cuposDisponibles: 30,
  horarios: [{ diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00' }],
  ...cambios,
});
