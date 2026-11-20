import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../service/auth.service';
import { enviarFormulario, errorDePrueba, escribir } from '../../util/pruebas.util';
import { RegistroComponent } from './registro.component';

/** Pruebas de la pantalla de registro. */
describe('RegistroComponent', () => {
  /** Ruta del registro en el backend. */
  const URL_REGISTRO = `${environment.apiUrl}/api/auth/registro`;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Enrutador espiado. */
  let navegar: ReturnType<typeof vi.spyOn>;

  /** Componente dibujado. */
  let fixture: ComponentFixture<RegistroComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

  /** Dibuja la pantalla con HTTP simulado. */
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [RegistroComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(RegistroComponent);
    await fixture.whenStable();
    html = fixture.nativeElement as HTMLElement;
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /**
   * Llena el formulario con datos válidos.
   *
   * @param documento documento a escribir
   */
  function llenar(documento = '1012345678'): void {
    escribir(html, 'documento', documento);
    escribir(html, 'nombreCompleto', 'Ana María Pérez');
    escribir(html, 'correo', 'ana@example.com');
    escribir(html, 'contrasena', 'secreto123');
    enviarFormulario(html);
  }

  /** Un registro aceptado lleva a verificar y recuerda el correo en memoria. */
  it('registra y lleva a verificar el código', () => {
    llenar();
    const peticion = backend.expectOne(URL_REGISTRO);
    expect(peticion.request.body).toEqual({
      documento: '1012345678',
      nombreCompleto: 'Ana María Pérez',
      correo: 'ana@example.com',
      contrasena: 'secreto123',
    });
    peticion.flush({ mensaje: 'Te enviamos un código' }, { status: 201, statusText: 'Created' });

    expect(TestBed.inject(AuthService).correoPendiente()).toBe('ana@example.com');
    expect(navegar).toHaveBeenCalledWith('/verificar');
  });

  /** Un documento que no son 10 dígitos se rechaza antes de llamar al backend. */
  it('valida el documento antes de enviar', async () => {
    llenar('123');
    await fixture.whenStable();

    backend.expectNone(URL_REGISTRO);
    expect(html.textContent).toContain('exactamente 10 dígitos');
  });

  /** Un documento repetido se marca en su propio campo. */
  it('marca el campo cuando el documento ya existe', async () => {
    llenar();
    backend
      .expectOne(URL_REGISTRO)
      .flush(errorDePrueba(409, 'DOCUMENTO_DUPLICADO', 'Ese documento ya está registrado'), {
        status: 409,
        statusText: 'Conflict',
      });
    await fixture.whenStable();

    expect(html.querySelector('.invalid-feedback')?.textContent).toContain(
      'Ese documento ya está registrado',
    );
    expect(navegar).not.toHaveBeenCalled();
  });

  /** Otros errores, como esperar antes de otro registro (429), aparecen como mensaje general. */
  it('muestra el mensaje general de un 429', async () => {
    llenar();
    backend
      .expectOne(URL_REGISTRO)
      .flush(errorDePrueba(429, 'DEMASIADOS_INTENTOS', 'Espera 60 segundos'), {
        status: 429,
        statusText: 'Too Many Requests',
      });
    await fixture.whenStable();

    expect(html.querySelector('.alert-danger')?.textContent).toContain('Espera 60 segundos');
  });
});
