import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { ProfesorService } from './profesor.service';

/** Pruebas del servicio del profesor. */
describe('ProfesorService', () => {
  /** Base de las rutas del profesor. */
  const BASE = `${environment.apiUrl}/api/profesor`;

  /** Servicio bajo prueba. */
  let servicio: ProfesorService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(ProfesorService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Sin periodo no envía el parámetro; con periodo lo envía. */
  it('consulta los grupos con o sin periodo', () => {
    servicio.grupos().subscribe();
    expect(backend.expectOne(`${BASE}/grupos`).request.params.has('periodoId')).toBe(false);
    servicio.grupos(2).subscribe();
    const peticion = backend.expectOne((r) => r.url === `${BASE}/grupos`);
    expect(peticion.request.params.get('periodoId')).toBe('2');
    peticion.flush([]);
  });

  /** La nota se envía con el corte y el valor al endpoint de la inscripción. */
  it('registra la nota de un corte', () => {
    servicio.registrarNota(7, { corte: 3, valor: 3.8 }).subscribe();
    const peticion = backend.expectOne(`${BASE}/inscripciones/7/notas`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ corte: 3, valor: 3.8 });
    peticion.flush({});
  });

  /** Estudiantes, horario e historial usan sus rutas del contrato. */
  it('consulta estudiantes, horario e historial', () => {
    servicio.estudiantes(10).subscribe();
    backend.expectOne(`${BASE}/grupos/10/estudiantes`).flush([]);
    servicio.horario().subscribe();
    backend.expectOne(`${BASE}/horario`).flush([]);
    servicio.historial(4).subscribe();
    backend.expectOne(`${BASE}/estudiantes/4/historial`).flush([]);
  });
});
