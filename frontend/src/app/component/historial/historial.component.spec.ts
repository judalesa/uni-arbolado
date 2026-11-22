import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { HistorialPeriodoDto } from '../../model/consulta.model';
import { PERIODOS_PRUEBA, grupoDePrueba } from '../../util/datos-prueba.util';
import { HistorialComponent, ordenarHistorial } from './historial.component';

/** Historial de ejemplo, del más antiguo al más reciente. */
const HISTORIAL: HistorialPeriodoDto[] = [
  {
    periodo: PERIODOS_PRUEBA[0],
    inscripciones: [
      {
        id: 1,
        grupo: grupoDePrueba({ periodo: PERIODOS_PRUEBA[0] }),
        estado: 'REPROBADA',
        notaCorte1: 2,
        notaCorte2: 2.5,
        notaCorte3: 2,
        notaDefinitiva: 2.2,
      },
    ],
  },
  {
    periodo: PERIODOS_PRUEBA[1],
    inscripciones: [{ id: 2, grupo: grupoDePrueba(), estado: 'CURSANDO' }],
  },
];

/** Pruebas del orden del historial. */
describe('ordenarHistorial', () => {
  /** El periodo más reciente va primero. */
  it('ordena del más reciente al más antiguo', () => {
    expect(ordenarHistorial(HISTORIAL).map((h) => h.periodo.codigo)).toEqual(['2026-2', '2026-1']);
  });
});

/** Pruebas del historial académico. */
describe('HistorialComponent', () => {
  /** Muestra un bloque por periodo con sus intentos y definitivas. */
  it('muestra los periodos con sus intentos', async () => {
    TestBed.configureTestingModule({
      imports: [HistorialComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(HistorialComponent);
    const html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/estudiante/historial`).flush(HISTORIAL);
    await fixture.whenStable();

    const bloques = html.querySelectorAll('section.card');
    expect(bloques).toHaveLength(2);
    expect(bloques[0].textContent).toContain('Periodo 2026-2');
    expect(bloques[0].textContent).toContain('Activo');
    expect(bloques[1].textContent).toContain('2.2');
    expect(bloques[1].textContent).toContain('Reprobada');
    expect(html.querySelector('button')).toBeNull();
    backend.verify();
  });

  /** Con el id del estudiante en la URL consulta como profesor y muestra su nombre. */
  it('muestra el historial de un estudiante al profesor', async () => {
    TestBed.configureTestingModule({
      imports: [HistorialComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '3' }) } },
        },
      ],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(HistorialComponent);
    const html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    const estudiante = { id: 3, documento: '1000000003', nombreCompleto: 'Luis Gómez' };
    backend
      .expectOne(`${environment.apiUrl}/api/profesor/estudiantes/3/historial`)
      .flush([
        {
          periodo: PERIODOS_PRUEBA[1],
          inscripciones: [{ id: 2, estudiante, grupo: grupoDePrueba(), estado: 'CURSANDO' }],
        },
      ]);
    await fixture.whenStable();

    expect(html.querySelector('h1')?.textContent).toContain('de Luis Gómez');
    expect(html.querySelector('button')?.textContent).toContain('Volver');
    backend.verify();
  });
});
