import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { grupoDePrueba } from '../../util/datos-prueba.util';
import { MisInscripcionesComponent } from './mis-inscripciones.component';

/** Pruebas de mis inscripciones y notas. */
describe('MisInscripcionesComponent', () => {
  /** Muestra las notas con un decimal, guion en los cortes pendientes y el estado. */
  it('muestra las notas por corte', async () => {
    TestBed.configureTestingModule({
      imports: [MisInscripcionesComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MisInscripcionesComponent);
    const html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend
      .expectOne((r) => r.url === `${environment.apiUrl}/api/estudiante/inscripciones`)
      .flush([{ id: 1, grupo: grupoDePrueba(), estado: 'CURSANDO', notaCorte1: 3, notaCorte2: null }]);
    await fixture.whenStable();

    const fila = html.querySelector('tbody tr')?.textContent ?? '';
    expect(html.querySelector('h1')?.textContent).toContain('2026-2');
    expect(fila).toContain('Marta Rojas');
    expect(fila).toContain('3.0');
    expect(fila).toContain('—');
    expect(fila).toContain('Cursando');
    backend.verify();
  });
});
