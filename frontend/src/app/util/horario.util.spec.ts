import { armarCuadricula, formatearHorarios, validarHorarios } from './horario.util';

/** Pruebas de las utilidades de horario. */
describe('horario.util', () => {
  /** El resumen ordena por día y hora, sin importar el orden de llegada. */
  it('resume las franjas ordenadas', () => {
    expect(
      formatearHorarios([
        { diaSemana: 'MIERCOLES', horaInicio: '08:00', horaFin: '10:00' },
        { diaSemana: 'LUNES', horaInicio: '10:00', horaFin: '12:00' },
        { diaSemana: 'LUNES', horaInicio: '07:00', horaFin: '09:00' },
      ]),
    ).toBe('Lu 07:00–09:00 · Lu 10:00–12:00 · Mi 08:00–10:00');
  });

  /** La cuadrícula tiene una fila por franja distinta y una columna por día de lunes a sábado. */
  it('arma la cuadrícula semanal', () => {
    /**
     * Crea una clase de prueba.
     *
     * @param diaSemana día de la clase
     * @param horaInicio hora de inicio
     * @param horaFin hora de fin
     * @return clase con datos fijos de asignatura
     */
    const clase =(diaSemana: 'LUNES' | 'MIERCOLES', horaInicio: string, horaFin: string) => ({
      asignatura: { id: 1, codigo: 'DSI301', nombre: 'Desarrollo 3', creditos: 4 },
      grupoNumero: 1,
      diaSemana,
      horaInicio,
      horaFin,
    });
    const filas = armarCuadricula([
      clase('MIERCOLES', '10:00', '12:00'),
      clase('LUNES', '08:00', '10:00'),
      clase('MIERCOLES', '08:00', '10:00'),
    ]);
    expect(filas.map((f) => f.franja)).toEqual(['08:00–10:00', '10:00–12:00']);
    expect(filas[0].dias).toHaveLength(6);
    expect(filas[0].dias[0]).toHaveLength(1);
    expect(filas[0].dias[1]).toHaveLength(0);
    expect(filas[0].dias[2]).toHaveLength(1);
    expect(filas[1].dias[0]).toHaveLength(0);
  });

  /** Sin franjas hay que agregar al menos una. */
  it('exige al menos un horario', () => {
    expect(validarHorarios([])).toEqual(['Agrega al menos un horario.']);
  });

  /** Señala la fila incompleta y la que termina antes de empezar. */
  it('rechaza franjas incompletas o invertidas', () => {
    expect(
      validarHorarios([
        { diaSemana: '', horaInicio: '08:00', horaFin: '10:00' },
        { diaSemana: 'MARTES', horaInicio: '10:00', horaFin: '10:00' },
      ]),
    ).toEqual([
      'Completa el día y las horas del horario 1.',
      'En el horario 2 la hora de fin debe ser posterior a la de inicio.',
    ]);
  });

  /** Dos franjas del mismo día que se cruzan se rechazan; si solo se tocan, no. */
  it('detecta traslapes del mismo día', () => {
    expect(
      validarHorarios([
        { diaSemana: 'JUEVES', horaInicio: '08:00', horaFin: '10:00' },
        { diaSemana: 'JUEVES', horaInicio: '09:00', horaFin: '11:00' },
      ]),
    ).toEqual(['Los horarios 1 y 2 se traslapan el jueves.']);
    expect(
      validarHorarios([
        { diaSemana: 'JUEVES', horaInicio: '08:00', horaFin: '10:00' },
        { diaSemana: 'JUEVES', horaInicio: '10:00', horaFin: '12:00' },
        { diaSemana: 'VIERNES', horaInicio: '08:00', horaFin: '10:00' },
      ]),
    ).toEqual([]);
  });
});
