import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

/** Pruebas del componente raíz. */
describe('App', () => {
  /** Prepara el entorno de pruebas con el componente raíz y un enrutador vacío. */
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  /** El componente raíz debe poder crearse. */
  it('se crea correctamente', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  /** El encabezado debe mostrar el nombre de la institución. */
  it('muestra el nombre de la institución en el encabezado', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.navbar-brand')?.textContent).toContain('Universidad El Arbolado');
  });
});
