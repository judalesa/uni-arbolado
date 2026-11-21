import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../service/auth.service';
import { SesionService } from '../../service/sesion.service';
import { enviarFormulario, errorDePrueba, escribir, loginDePrueba } from '../../util/pruebas.util';
import { LoginComponent } from './login.component';

/** Pruebas de la pantalla de inicio de sesión. */
describe('LoginComponent', () => {
  /** Ruta del login en el backend. */
  const URL_LOGIN = `${environment.apiUrl}/api/auth/login`;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Enrutador espiado. */
  let navegar: ReturnType<typeof vi.spyOn>;

  /** Componente dibujado. */
  let fixture: ComponentFixture<LoginComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

  /** Dibuja la pantalla con HTTP simulado y la sesión limpia. */
  beforeEach(async () => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginComponent);
    await fixture.whenStable();
    html = fixture.nativeElement as HTMLElement;
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Escribe credenciales y envía el formulario. */
  function ingresar(): void {
    escribir(html, 'correo', 'ana@example.com');
    escribir(html, 'contrasena', 'secreto123');
    enviarFormulario(html);
  }

  /** Con credenciales correctas guarda la sesión y va a la pantalla del rol. */
  it('inicia sesión y va a la pantalla inicial del rol', () => {
    ingresar();
    const peticion = backend.expectOne(URL_LOGIN);
    expect(peticion.request.body).toEqual({ correo: 'ana@example.com', contrasena: 'secreto123' });
    peticion.flush(loginDePrueba('PROFESOR'));

    expect(TestBed.inject(SesionService).rol()).toBe('PROFESOR');
    expect(navegar).toHaveBeenCalledWith('/profesor/grupos');
  });

  /** Credenciales incorrectas se muestran en la pantalla y no abren sesión. */
  it('muestra el error de credenciales inválidas', async () => {
    ingresar();
    backend
      .expectOne(URL_LOGIN)
      .flush(errorDePrueba(401, 'CREDENCIALES_INVALIDAS', 'Correo o contraseña incorrectos'), {
        status: 401,
        statusText: 'Unauthorized',
      });
    await fixture.whenStable();

    expect(html.querySelector('.alert-danger')?.textContent).toContain(
      'Correo o contraseña incorrectos',
    );
    expect(TestBed.inject(SesionService).estaVigente()).toBe(false);
    expect(navegar).not.toHaveBeenCalled();
  });

  /** Una cuenta pendiente se lleva a verificar con su correo recordado en memoria. */
  it('lleva a verificar si la cuenta está pendiente', () => {
    ingresar();
    backend
      .expectOne(URL_LOGIN)
      .flush(errorDePrueba(403, 'CUENTA_PENDIENTE'), { status: 403, statusText: 'Forbidden' });

    expect(TestBed.inject(AuthService).correoPendiente()).toBe('ana@example.com');
    expect(navegar).toHaveBeenCalledWith('/verificar');
  });

  /** Con campos vacíos no se llama al backend. */
  it('no envía si faltan datos', () => {
    enviarFormulario(html);

    backend.expectNone(URL_LOGIN);
  });
});
