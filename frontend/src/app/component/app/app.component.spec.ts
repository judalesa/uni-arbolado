import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AppComponent } from './app.component';

/** Pruebas del componente raíz. */
describe('AppComponent', () => {
  /** Prepara el entorno de pruebas con el componente raíz y un enrutador vacío. */
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  /** El componente raíz debe poder crearse. */
  it('se crea correctamente', () => {
    const fixture = TestBed.createComponent(AppComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  /** El encabezado debe mostrar el nombre de la institución. */
  it('muestra el nombre de la institución en el encabezado', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.navbar-brand')?.textContent).toContain('Universidad El Arbolado');
  });
});
