import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { AuditoriaService } from './auditoria.service';

/** Pruebas del servicio de auditoría. */
describe('AuditoriaService', () => {
  /** Ruta de la auditoría. */
  const URL = `${environment.apiUrl}/api/admin/auditoria`;

  /** Servicio bajo prueba. */
  let servicio: AuditoriaService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(AuditoriaService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Envía solo los filtros con valor, más la página y el tamaño. */
  it('omite los filtros vacíos', () => {
    servicio
      .consultar(
        { usuarioId: 5, accion: 'INSCRIPCION', resultado: null, desde: '', hasta: undefined },
        2,
        20,
      )
      .subscribe();
    const peticion = backend.expectOne((r) => r.url === URL);
    expect(peticion.request.params.keys().sort()).toEqual([
      'accion',
      'pagina',
      'tamano',
      'usuarioId',
    ]);
    expect(peticion.request.params.get('usuarioId')).toBe('5');
    expect(peticion.request.params.get('pagina')).toBe('2');
    peticion.flush({ contenido: [], pagina: 2, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });
});
