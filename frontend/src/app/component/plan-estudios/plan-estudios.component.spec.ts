import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AsignaturaDto } from '../../model/asignatura.model';
import { PlanEstudiosComponent, agruparPorSemestre } from './plan-estudios.component';

/** Plan reducido de ejemplo, en desorden. */
const PLAN: AsignaturaDto[] = [
  {
    id: 2,
    codigo: 'DSI201',
    nombre: 'Desarrollo 2',
    semestre: 2,
    creditos: 4,
    prerrequisitos: [{ id: 1, codigo: 'DSI101', nombre: 'Desarrollo 1', creditos: 4 }],
  },
  { id: 3, codigo: 'MAT101', nombre: 'Cálculo', semestre: 1, creditos: 3, prerrequisitos: [] },
  { id: 1, codigo: 'DSI101', nombre: 'Desarrollo 1', semestre: 1, creditos: 4, prerrequisitos: [] },
];

/** Pruebas del plan de estudios del administrador. */
describe('PlanEstudiosComponent', () => {
  /** Agrupa por semestre en orden, ordena por código y suma créditos. */
  it('agrupa por semestre', () => {
    const semestres = agruparPorSemestre(PLAN);

    expect(semestres.map((s) => s.numero)).toEqual([1, 2]);
    expect(semestres[0].asignaturas.map((a) => a.codigo)).toEqual(['DSI101', 'MAT101']);
    expect(semestres[0].creditos).toBe(7);
  });

  /** Muestra un bloque por semestre, los totales y los prerrequisitos por código. */
  it('dibuja el plan', async () => {
    TestBed.configureTestingModule({
      imports: [PlanEstudiosComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PlanEstudiosComponent);
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/asignaturas`).flush(PLAN);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    expect(html.querySelectorAll('section.card').length).toBe(2);
    expect(html.textContent).toContain('3 asignaturas · 11 créditos');
    expect(html.textContent).toContain('DSI101');
    backend.verify();
  });
});
