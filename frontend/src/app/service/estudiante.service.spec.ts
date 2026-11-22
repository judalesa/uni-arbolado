import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { EstudianteService } from './estudiante.service';

/** Pruebas del servicio de consultas del estudiante. */
describe('EstudianteService', () => {
  /** Base de las consultas del estudiante. */
  const BASE = `${environment.apiUrl}/api/estudiante`;

  /** Servicio bajo prueba. */
  let servicio: EstudianteService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(EstudianteService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Cada consulta usa su ruta del contrato. */
  it('consulta plan, horario e historial', () => {
    servicio.planEstado().subscribe();
    backend.expectOne(`${BASE}/plan-estado`).flush([]);
    servicio.horario().subscribe();
    backend.expectOne(`${BASE}/horario`).flush([]);
    servicio.historial().subscribe();
    backend.expectOne(`${BASE}/historial`).flush([]);
  });
});
