import { formatearNota } from './notas.util';

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
