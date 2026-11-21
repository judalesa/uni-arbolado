import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { PeriodoService } from './periodo.service';

/** Pruebas del servicio de periodos. */
describe('PeriodoService', () => {
  /** Base de las rutas de periodos. */
  const BASE = `${environment.apiUrl}/api/periodos`;

  /** Cuerpo de ejemplo. */
  const DATOS = { codigo: '2026-2', fechaInicio: '2026-07-20', fechaFin: '2026-12-05' };

  /** Servicio bajo prueba. */
  let servicio: PeriodoService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(PeriodoService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Cada operación usa el método y la ruta del contrato. */
  it('usa las rutas del contrato', () => {
    servicio.listar().subscribe();
    servicio.crear(DATOS).subscribe();
    servicio.modificar(7, DATOS).subscribe();
    servicio.activar(7).subscribe();

    backend.expectOne({ method: 'GET', url: BASE }).flush([]);
    backend.expectOne({ method: 'POST', url: BASE }).flush({});
    backend.expectOne({ method: 'PUT', url: `${BASE}/7` }).flush({});
    backend.expectOne({ method: 'POST', url: `${BASE}/7/activar` }).flush({});
  });
});
