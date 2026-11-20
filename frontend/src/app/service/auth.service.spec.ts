import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../environments/environment';
import { loginDePrueba } from '../util/pruebas.util';
import { AuthService } from './auth.service';
import { SesionService } from './sesion.service';

/** Pruebas del servicio de autenticación. */
describe('AuthService', () => {
  /** Servicio a probar. */
  let auth: AuthService;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Enrutador espiado. */
  let navegar: ReturnType<typeof vi.spyOn>;

  /** Prepara el servicio con HTTP simulado y la sesión limpia. */
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Cada operación llama a su ruta del contrato con el cuerpo recibido. */
  it('llama a las rutas de autenticación del contrato', () => {
    const base = `${environment.apiUrl}/api/auth`;
    auth.login({ correo: 'a@b.co', contrasena: 'x' }).subscribe();
    auth
      .registrar({
        documento: '1012345678',
        nombreCompleto: 'Ana',
        correo: 'a@b.co',
        contrasena: 'x',
      })
      .subscribe();
    auth.verificar({ correo: 'a@b.co', codigo: '123456' }).subscribe();
    auth.reenviar({ correo: 'a@b.co' }).subscribe();

    expect(backend.expectOne(`${base}/login`).request.method).toBe('POST');
    expect(backend.expectOne(`${base}/registro`).request.body.documento).toBe('1012345678');
    expect(backend.expectOne(`${base}/verificar`).request.body).toEqual({
      correo: 'a@b.co',
      codigo: '123456',
    });
    expect(backend.expectOne(`${base}/reenviar-codigo`).request.body).toEqual({ correo: 'a@b.co' });
  });

  /** Al salir se avisa al backend, se borra la sesión y se vuelve al login. */
  it('cierra la sesión en el servidor y en el navegador', () => {
    const sesion = TestBed.inject(SesionService);
    sesion.iniciar(loginDePrueba());

    auth.salir();
    backend
      .expectOne(`${environment.apiUrl}/api/auth/logout`)
      .flush(null, { status: 204, statusText: 'No Content' });

    expect(sesion.estaVigente()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
  });

  /** Aunque el servidor falle, el usuario no queda atrapado con una sesión local. */
  it('cierra la sesión local aunque falle el servidor', () => {
    const sesion = TestBed.inject(SesionService);
    sesion.iniciar(loginDePrueba());

    auth.salir();
    backend
      .expectOne(`${environment.apiUrl}/api/auth/logout`)
      .flush(null, { status: 500, statusText: 'Server Error' });

    expect(sesion.estaVigente()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
  });
});
