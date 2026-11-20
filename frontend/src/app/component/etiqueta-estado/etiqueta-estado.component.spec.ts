import { TestBed } from '@angular/core/testing';

import { EtiquetaEstadoComponent } from './etiqueta-estado.component';

/** Pruebas de la etiqueta de estado. */
describe('EtiquetaEstadoComponent', () => {
  /**
   * Dibuja la etiqueta de un estado.
   *
   * @param estado código del estado
   * @return el elemento `span` resultante
   */
  async function dibujar(estado: string): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(EtiquetaEstadoComponent);
    fixture.componentRef.setInput('estado', estado);
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).querySelector('span') as HTMLElement;
  }

  /** Cada estado de asignatura tiene su color (mapa de pantallas, sección 2). */
  it('usa el color de cada estado', async () => {
    expect((await dibujar('POR_CURSAR')).classList).toContain('text-bg-secondary');
    expect((await dibujar('CURSANDO')).classList).toContain('text-bg-primary');
    expect((await dibujar('APROBADA')).classList).toContain('text-bg-success');
    expect((await dibujar('REPROBADA')).classList).toContain('text-bg-danger');
  });

  /** Un estado desconocido se muestra con su propio código. */
  it('muestra un estado desconocido en gris', async () => {
    const etiqueta = await dibujar('OTRO');

    expect(etiqueta.textContent).toBe('OTRO');
    expect(etiqueta.classList).toContain('text-bg-secondary');
  });
});
