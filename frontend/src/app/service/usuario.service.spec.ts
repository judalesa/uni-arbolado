import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { UsuarioService } from './usuario.service';

/** Pruebas del servicio de usuarios del administrador. */
describe('UsuarioService', () => {
  /** Servicio bajo prueba. */
  let servicio: UsuarioService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Prepara el servicio con HTTP simulado. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(UsuarioService);
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Cada rol usa su propia ruta y la búsqueda viaja recortada en `q`. */
  it('lista por rol con búsqueda y paginación', () => {
    servicio.listar('PROFESOR', '  rojas ', 2, 10).subscribe();
    const peticion = backend.expectOne((r) => r.url === `${environment.apiUrl}/api/admin/profesores`);
    expect(peticion.request.params.get('q')).toBe('rojas');
    expect(peticion.request.params.get('pagina')).toBe('2');
    expect(peticion.request.params.get('tamano')).toBe('10');
    peticion.flush({ contenido: [], pagina: 2, tamano: 10, totalElementos: 0, totalPaginas: 0 });
  });

  /** Sin texto de búsqueda no se envía el parámetro `q`. */
  it('omite la búsqueda vacía', () => {
    servicio.listar('ESTUDIANTE').subscribe();
    const peticion = backend.expectOne((r) => r.url === `${environment.apiUrl}/api/admin/estudiantes`);
    expect(peticion.request.params.has('q')).toBe(false);
    peticion.flush({ contenido: [], pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 });
  });

  /** La creación de profesores es un POST con el cuerpo del contrato. */
  it('crea un profesor', () => {
    const datos = {
      documento: '1012345678',
      nombreCompleto: 'Marta Rojas',
      correo: 'marta@example.com',
      contrasena: 'secreto123',
    };
    servicio.crearProfesor(datos).subscribe();
    const peticion = backend.expectOne(`${environment.apiUrl}/api/admin/profesores`);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(datos);
    peticion.flush({});
  });

  /** El perfil propio se lee de `/api/usuarios/me`, para cualquier rol. */
  it('consulta el perfil propio', () => {
    servicio.yo().subscribe();
    backend.expectOne({ method: 'GET', url: `${environment.apiUrl}/api/usuarios/me` }).flush({});
  });
});
