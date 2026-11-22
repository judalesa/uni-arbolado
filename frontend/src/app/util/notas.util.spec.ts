import { formatearNota, leerNota } from './notas.util';

/** Pruebas del formato de notas. */
describe('formatearNota', () => {
  /** Siempre un decimal; sin nota, un guion. */
  it('muestra un decimal o guion', () => {
    expect(formatearNota(3)).toBe('3.0');
    expect(formatearNota(4.5)).toBe('4.5');
    expect(formatearNota(0)).toBe('0.0');
    expect(formatearNota(null)).toBe('—');
    expect(formatearNota()).toBe('—');
  });
});

/** Pruebas de la lectura de la nota escrita por el profesor. */
describe('leerNota', () => {
  /** Acepta enteros y un decimal con punto o coma, de 0 a 5. */
  it('acepta notas válidas', () => {
    expect(leerNota('3.8')).toBe(3.8);
    expect(leerNota(' 4,5 ')).toBe(4.5);
    expect(leerNota('0')).toBe(0);
    expect(leerNota('5.0')).toBe(5);
  });

  /** Rechaza vacíos, fuera de rango, más de un decimal y texto. */
  it('rechaza notas inválidas', () => {
    expect(leerNota('')).toBeNull();
    expect(leerNota('5.1')).toBeNull();
    expect(leerNota('-1')).toBeNull();
    expect(leerNota('3.75')).toBeNull();
    expect(leerNota('10')).toBeNull();
    expect(leerNota('abc')).toBeNull();
  });
});
