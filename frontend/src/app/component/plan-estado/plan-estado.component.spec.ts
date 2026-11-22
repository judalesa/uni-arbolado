import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AsignaturaEstadoDto } from '../../model/consulta.model';
import { PlanEstadoComponent, calcularAvance } from './plan-estado.component';

/**
 * Fila del plan con estado.
 *
 * @param id identificador de la asignatura
 * @param semestre semestre
 * @param estado estado para el estudiante
 * @param notaDefinitiva definitiva, si la hay
 * @return fila del plan
 */
const fila = (
  id: number,
  semestre: number,
  estado: AsignaturaEstadoDto['estado'],
  notaDefinitiva: number | null = null,
): AsignaturaEstadoDto => ({
  asignatura: { id, codigo: `ASG${id}`, nombre: `Asignatura ${id}`, semestre, creditos: 3, prerrequisitos: [] },
  estado,
  notaDefinitiva,
});

/** Plan de ejemplo. */
const PLAN = [fila(1, 1, 'APROBADA', 4), fila(2, 1, 'REPROBADA', 2.5), fila(3, 2, 'POR_CURSAR')];

/** Pruebas del avance del estudiante. */
describe('calcularAvance', () => {
  /** Solo las aprobadas suman al avance. */
  it('cuenta aprobadas y créditos', () => {
    expect(calcularAvance(PLAN)).toEqual({
      aprobadas: 1,
      total: 3,
      creditosAprobados: 3,
      creditosTotales: 9,
    });
  });
});

/** Pruebas del plan con estado. */
describe('PlanEstadoComponent', () => {
  /** Muestra cada semestre con la etiqueta de estado y la definitiva con un decimal. */
  it('muestra el plan con estados', async () => {
    TestBed.configureTestingModule({
      imports: [PlanEstadoComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PlanEstadoComponent);
    const html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/estudiante/plan-estado`).flush(PLAN);
    await fixture.whenStable();

    expect(html.querySelectorAll('section.card')).toHaveLength(2);
    expect(html.textContent).toContain('1 de 3 asignaturas aprobadas');
    const filas = html.querySelectorAll('tbody tr');
    expect(filas[0].textContent).toContain('4.0');
    expect(filas[0].textContent).toContain('Aprobada');
    expect(filas[1].textContent).toContain('Reprobada');
    expect(filas[2].textContent).toContain('Por cursar');
    backend.verify();
  });
});
