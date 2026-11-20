import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../service/auth.service';
import { enviarFormulario, errorDePrueba, escribir } from '../../util/pruebas.util';
import { VerificarComponent } from './verificar.component';

/** Pruebas de la pantalla de verificación del correo. */
describe('VerificarComponent', () => {
  /** Base de las rutas de autenticación. */
  const BASE = `${environment.apiUrl}/api/auth`;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Enrutador espiado. */
  let navegar: ReturnType<typeof vi.spyOn>;

  /** Prepara HTTP simulado y un enrutador que no navega de verdad. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [VerificarComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /**
   * Dibuja la pantalla, con o sin correo conocido.
   *
   * @param correo correo recordado por el registro o el login, o `null`
   * @return el componente dibujado
   */
  async function dibujar(correo: string | null): Promise<ComponentFixture<VerificarComponent>> {
    TestBed.inject(AuthService).correoPendiente.set(correo);
    const fixture = TestBed.createComponent(VerificarComponent);
    await fixture.whenStable();
    return fixture;
  }

  /** Sin correo conocido se pide primero el correo. */
  it('pide el correo si no se conoce', async () => {
    const fixture = await dibujar(null);
    const html = fixture.nativeElement as HTMLElement;
    escribir(html, 'correo', 'ana@example.com');
    enviarFormulario(html);
    await fixture.whenStable();

    expect(html.textContent).toContain('ana@example.com');
    expect(html.querySelector('#campo-codigo')).not.toBeNull();
  });

  /** Un código correcto activa la cuenta y lleva al login. */
  it('verifica el código y va al login', async () => {
    const fixture = await dibujar('ana@example.com');
    const html = fixture.nativeElement as HTMLElement;
    escribir(html, 'codigo', '482913');
    enviarFormulario(html);

    const peticion = backend.expectOne(`${BASE}/verificar`);
    expect(peticion.request.body).toEqual({ correo: 'ana@example.com', codigo: '482913' });
    peticion.flush({ mensaje: 'Cuenta activada' });

    expect(TestBed.inject(AuthService).correoPendiente()).toBeNull();
    expect(navegar).toHaveBeenCalledWith('/login');
  });

  /** Un código que no son 6 dígitos no se envía. */
  it('no envía un código mal formado', async () => {
    const fixture = await dibujar('ana@example.com');
    const html = fixture.nativeElement as HTMLElement;
    escribir(html, 'codigo', '12');
    enviarFormulario(html);

    backend.expectNone(`${BASE}/verificar`);
  });

  /** Un código vencido o inválido (422) se muestra y no avanza. */
  it('muestra el error de un código inválido', async () => {
    const fixture = await dibujar('ana@example.com');
    const html = fixture.nativeElement as HTMLElement;
    escribir(html, 'codigo', '000000');
    enviarFormulario(html);
    backend
      .expectOne(`${BASE}/verificar`)
      .flush(errorDePrueba(422, 'CODIGO_INVALIDO', 'El código no es válido'), {
        status: 422,
        statusText: 'Unprocessable Entity',
      });
    await fixture.whenStable();

    expect(html.querySelector('.alert-danger')?.textContent).toContain('El código no es válido');
    expect(navegar).not.toHaveBeenCalled();
  });

  /** Tras reenviar, el botón queda bloqueado 60 s con cuenta regresiva (propuesta P-02). */
  it('reenvía el código y bloquea el botón durante 60 segundos', async () => {
    const fixture = await dibujar('ana@example.com');
    const html = fixture.nativeElement as HTMLElement;
    vi.useFakeTimers();
    try {
      const boton = Array.from(html.querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Reenviar'),
      ) as HTMLButtonElement;
      boton.click();
      backend.expectOne(`${BASE}/reenviar-codigo`).flush({ mensaje: 'Código reenviado' });
      fixture.detectChanges();

      expect(boton.disabled).toBe(true);
      expect(boton.textContent).toContain('60 s');

      vi.advanceTimersByTime(60_000);
      fixture.detectChanges();

      expect(boton.disabled).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });
});
