import { formatearHorarios, validarHorarios } from './horario.util';

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
