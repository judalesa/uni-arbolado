import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { InscripcionService } from './inscripcion.service';

/** Pruebas del servicio de inscripciones del estudiante. */
describe('InscripcionService', () => {
  /** Servicio bajo prueba. */
  let servicio: InscripcionService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(InscripcionService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Inscribir envía solo el grupo: el estudiante sale del token. */
  it('inscribe con el cuerpo del contrato', () => {
    servicio.inscribir(10).subscribe();
    const peticion = backend.expectOne({ method: 'POST', url: `${environment.apiUrl}/api/inscripciones` });
    expect(peticion.request.body).toEqual({ grupoId: 10 });
    peticion.flush({});
  });

  /** Sin periodo se consultan las del activo; con periodo, viaja como parámetro. */
  it('consulta mis inscripciones', () => {
    const ruta = `${environment.apiUrl}/api/estudiante/inscripciones`;
    servicio.mias().subscribe();
    const activo = backend.expectOne((r) => r.url === ruta);
    expect(activo.request.params.has('periodoId')).toBe(false);
    activo.flush([]);

    servicio.mias(3).subscribe();
    backend.expectOne((r) => r.url === ruta && r.params.get('periodoId') === '3').flush([]);
  });
});
