import { TestBed } from '@angular/core/testing';

import { Inicio } from './inicio';

/** Pruebas de la pantalla de bienvenida. */
describe('Inicio', () => {
  /** Prepara el entorno de pruebas de Angular con el componente a probar. */
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Inicio] }).compileComponents();
  });

  /** La pantalla debe mostrar el título del sistema. */
  it('muestra el título del sistema', async () => {
    const fixture = TestBed.createComponent(Inicio);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('h1')?.textContent).toContain('Sistema de Gestión Académica en Línea');
  });
});
