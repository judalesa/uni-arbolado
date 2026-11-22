import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { PERIODOS_PRUEBA, grupoDePrueba } from '../../util/datos-prueba.util';
import { MisGruposComponent, periodosDeGrupos } from './mis-grupos.component';

/** Grupos de ejemplo: uno en el periodo anterior y dos en el activo. */
const GRUPOS = [
  grupoDePrueba({ id: 1, periodo: PERIODOS_PRUEBA[0] }),
  grupoDePrueba({ id: 2, numero: 2 }),
  grupoDePrueba({ id: 3, numero: 1, inscritos: 12 }),
];

/** Pruebas de los periodos del filtro. */
describe('periodosDeGrupos', () => {
  /** Sin repetir y del más reciente al más antiguo. */
  it('saca los periodos distintos', () => {
    expect(periodosDeGrupos(GRUPOS).map((periodo) => periodo.codigo)).toEqual(['2026-2', '2026-1']);
  });
});

/** Pruebas de la pantalla de mis grupos. */
describe('MisGruposComponent', () => {
  /** Abre en el periodo activo y enlaza cada grupo con su registro de notas. */
  it('muestra los grupos del periodo activo', async () => {
    TestBed.configureTestingModule({
      imports: [MisGruposComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MisGruposComponent);
    const html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/profesor/grupos`).flush(GRUPOS);
    await fixture.whenStable();

    const filas = html.querySelectorAll('tbody tr');
    expect(filas).toHaveLength(2);
    expect(filas[0].textContent).toContain('12/30');
    expect(filas[0].querySelector('a')?.getAttribute('href')).toBe('/profesor/grupos/3');
    expect(html.querySelector<HTMLSelectElement>('#filtro-periodo')?.options).toHaveLength(2);
    backend.verify();
  });
});
