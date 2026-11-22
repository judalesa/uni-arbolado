import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { GrupoRequest } from '../model/grupo.model';
import { GrupoService } from './grupo.service';

/** Pruebas del servicio de grupos del administrador. */
describe('GrupoService', () => {
  /** Base de las rutas de grupos. */
  const BASE = `${environment.apiUrl}/api/admin/grupos`;

  /** Cuerpo de ejemplo. */
  const DATOS: GrupoRequest = {
    asignaturaId: 1,
    periodoId: 2,
    profesorId: 3,
    bloque: 'B',
    salon: '301',
    horarios: [{ diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00' }],
  };

  /** Servicio bajo prueba. */
  let servicio: GrupoService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(GrupoService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** El periodo viaja como parámetro solo si se indica. */
  it('lista con y sin periodo', () => {
    servicio.listar(2).subscribe();
    const conPeriodo = backend.expectOne((r) => r.url === BASE && r.params.get('periodoId') === '2');
    conPeriodo.flush([]);

    servicio.listar().subscribe();
    const todos = backend.expectOne((r) => r.url === BASE);
    expect(todos.request.params.has('periodoId')).toBe(false);
    todos.flush([]);
  });

  /** Consultar, crear y modificar usan el método y la ruta del contrato. */
  it('consulta, crea y modifica', () => {
    servicio.obtener(7).subscribe();
    backend.expectOne({ method: 'GET', url: `${BASE}/7` }).flush({});

    servicio.crear(DATOS).subscribe();
    const creacion = backend.expectOne({ method: 'POST', url: BASE });
    expect(creacion.request.body).toEqual(DATOS);
    creacion.flush({});

    servicio.modificar(7, DATOS).subscribe();
    backend.expectOne({ method: 'PUT', url: `${BASE}/7` }).flush({});
  });

  /** La oferta del estudiante usa su propia ruta, fuera de `/admin`. */
  it('consulta la oferta', () => {
    servicio.oferta().subscribe();
    backend.expectOne({ method: 'GET', url: `${environment.apiUrl}/api/grupos/oferta` }).flush([]);
  });
});
