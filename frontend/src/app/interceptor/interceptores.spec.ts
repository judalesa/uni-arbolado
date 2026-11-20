import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../environments/environment';
import { erroresInterceptor } from './errores.interceptor';
import { loginDePrueba } from '../util/pruebas.util';
import { SesionService } from '../service/sesion.service';
import { ToastService } from '../service/toast.service';
import { tokenInterceptor } from './token.interceptor';

/** Pruebas de los interceptores de token y de errores. */
describe('interceptores HTTP', () => {
  /** Cliente HTTP de la prueba. */
  let http: HttpClient;

  /** Controlador que responde las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Sesión de la prueba. */
  let sesion: SesionService;

  /** Enrutador espiado para comprobar redirecciones. */
  let navegar: ReturnType<typeof vi.spyOn>;

  /** Prepara el cliente con ambos interceptores y una sesión vacía. */
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([tokenInterceptor, erroresInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionService);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Con sesión, las peticiones al backend llevan el token. */
  it('agrega el token a las peticiones al backend', () => {
    sesion.iniciar(loginDePrueba());
    http.get(`${environment.apiUrl}/api/usuarios/me`).subscribe();

    const peticion = backend.expectOne(`${environment.apiUrl}/api/usuarios/me`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer token-de-prueba');
    peticion.flush({});
  });

  /** Sin sesión no se envía encabezado. */
  it('no agrega encabezado sin sesión', () => {
    http.get(`${environment.apiUrl}/api/periodos/activo`).subscribe();

    const peticion = backend.expectOne(`${environment.apiUrl}/api/periodos/activo`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  /** El token no viaja a servidores distintos del backend. */
  it('no envía el token a otro servidor', () => {
    sesion.iniciar(loginDePrueba());
    http.get('https://otro-sitio.example.com/datos').subscribe();

    const peticion = backend.expectOne('https://otro-sitio.example.com/datos');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  /** Un 401 de token vencido cierra la sesión y lleva al login. */
  it('cierra la sesión y va al login ante un 401', () => {
    sesion.iniciar(loginDePrueba());
    http.get(`${environment.apiUrl}/api/usuarios/me`).subscribe({ error: () => undefined });

    backend
      .expectOne(`${environment.apiUrl}/api/usuarios/me`)
      .flush(cuerpo(401, 'NO_AUTENTICADO'), { status: 401, statusText: 'Unauthorized' });

    expect(sesion.estaVigente()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
  });

  /** Las credenciales incorrectas no cierran sesión ni redirigen: lo muestra la pantalla de login. */
  it('no redirige cuando las credenciales son inválidas', () => {
    http.post(`${environment.apiUrl}/api/auth/login`, {}).subscribe({ error: () => undefined });

    backend
      .expectOne(`${environment.apiUrl}/api/auth/login`)
      .flush(cuerpo(401, 'CREDENCIALES_INVALIDAS'), { status: 401, statusText: 'Unauthorized' });

    expect(navegar).not.toHaveBeenCalled();
  });

  /** Un 403 por rol lleva a la página de acceso denegado. */
  it('va a /403 ante un acceso denegado', () => {
    http.get(`${environment.apiUrl}/api/admin/grupos`).subscribe({ error: () => undefined });

    backend
      .expectOne(`${environment.apiUrl}/api/admin/grupos`)
      .flush(cuerpo(403, 'ACCESO_DENEGADO'), { status: 403, statusText: 'Forbidden' });

    expect(navegar).toHaveBeenCalledWith('/403');
  });

  /** La cuenta pendiente es un 403 que resuelve la pantalla de login, no el interceptor. */
  it('no redirige cuando la cuenta está pendiente', () => {
    http.post(`${environment.apiUrl}/api/auth/login`, {}).subscribe({ error: () => undefined });

    backend
      .expectOne(`${environment.apiUrl}/api/auth/login`)
      .flush(cuerpo(403, 'CUENTA_PENDIENTE'), { status: 403, statusText: 'Forbidden' });

    expect(navegar).not.toHaveBeenCalled();
  });

  /** Un error del servidor muestra un aviso y la pantalla igualmente recibe el error. */
  it('avisa ante un 500 y propaga el error', () => {
    let recibido = false;
    http.get(`${environment.apiUrl}/api/periodos`).subscribe({ error: () => (recibido = true) });

    backend
      .expectOne(`${environment.apiUrl}/api/periodos`)
      .flush(cuerpo(500, 'ERROR_INTERNO'), { status: 500, statusText: 'Server Error' });

    expect(recibido).toBe(true);
    expect(TestBed.inject(ToastService).avisos()).toHaveLength(1);
  });

  /**
   * Arma un cuerpo de error con el formato del contrato.
   *
   * @param status código HTTP
   * @param codigo código estable del error
   * @return cuerpo de error
   */
  function cuerpo(status: number, codigo: string) {
    return {
      timestamp: '2026-11-19T10:00:00Z',
      status,
      codigo,
      mensaje: 'Mensaje',
      ruta: '/api/x',
    };
  }
});
