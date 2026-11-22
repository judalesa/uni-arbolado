import { aInstante, formatearFecha, formatearInstante } from './fechas.util';

/** Pruebas del formato de fechas. */
describe('fechas.util', () => {
  /** Una fecha sin hora se muestra el mismo día, sin correrse por la zona horaria. */
  it('formatea una fecha sin hora sin cambiar el día', () => {
    expect(formatearFecha('2026-01-19')).toBe('19/01/2026');
  });

  /** Sin fecha o con un formato desconocido no se muestra nada. */
  it('devuelve vacío sin fecha válida', () => {
    expect(formatearFecha(null)).toBe('');
    expect(formatearFecha('19-01-2026')).toBe('');
  });

  /** Un instante válido produce texto y uno inválido, vacío. */
  it('formatea instantes válidos e ignora los inválidos', () => {
    expect(formatearInstante('2026-11-20T15:00:00Z')).not.toBe('');
    expect(formatearInstante('no-es-fecha')).toBe('');
    expect(formatearInstante(null)).toBe('');
  });

  /** La hora local del campo se convierte en el mismo instante en UTC; vacío da nulo. */
  it('convierte la hora local en un instante con zona', () => {
    const instante = aInstante('2026-11-20T08:30');
    expect(instante).toMatch(/Z$/);
    expect(new Date(instante ?? '').getTime()).toBe(new Date(2026, 10, 20, 8, 30).getTime());
    expect(aInstante('')).toBeNull();
    expect(aInstante('no-es-fecha')).toBeNull();
  });
});
