import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { HorarioComponent } from './horario.component';

/** Pruebas del horario semanal. */
describe('HorarioComponent', () => {
  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<HorarioComponent>;

  /** Dibuja la pantalla. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HorarioComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(HorarioComponent);
    fixture.detectChanges();
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Cada clase aparece en la columna de su día y la fila de su franja. */
  it('ubica las clases en la cuadrícula', async () => {
    backend.expectOne(`${environment.apiUrl}/api/estudiante/horario`).flush([
      {
        asignatura: { id: 1, codigo: 'DSI301', nombre: 'Desarrollo 3', creditos: 4 },
        grupoNumero: 2,
        bloque: 'B',
        salon: '301',
        diaSemana: 'MIERCOLES',
        horaInicio: '08:00',
        horaFin: '10:00',
      },
    ]);
    await fixture.whenStable();

    const html = fixture.nativeElement as HTMLElement;
    const celdas = html.querySelectorAll('tbody tr td');
    expect(html.querySelector('tbody th')?.textContent).toContain('08:00–10:00');
    expect(celdas).toHaveLength(6);
    expect(celdas[2].textContent).toContain('Desarrollo 3');
    expect(celdas[2].textContent).toContain('G2 · B-301');
    expect(celdas[0].textContent?.trim()).toBe('');
  });

  /** Sin clases se muestra un mensaje en lugar de la cuadrícula. */
  it('avisa si no hay clases', async () => {
    backend.expectOne(`${environment.apiUrl}/api/estudiante/horario`).flush([]);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No tienes clases');
  });
});
