import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SesionService } from '../../service/sesion.service';
import { loginDePrueba } from '../../util/pruebas.util';
import { InicioComponent } from './inicio.component';

/** Pruebas de la pantalla de bienvenida. */
describe('InicioComponent', () => {
  /** Prepara el entorno con un enrutador vacío y la sesión limpia. */
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [InicioComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  /**
   * Dibuja la pantalla.
   *
   * @return el elemento raíz
   */
  async function dibujar(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(InicioComponent);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  /** La pantalla debe mostrar el título del sistema. */
  it('muestra el título del sistema', async () => {
    expect((await dibujar()).querySelector('h1')?.textContent).toContain(
      'Sistema de Gestión Académica en Línea',
    );
  });

  /** Sin sesión se ofrece iniciar sesión y registrarse. */
  it('ofrece iniciar sesión y registrarse sin sesión', async () => {
    const enlaces = Array.from((await dibujar()).querySelectorAll('a')).map((a) =>
      a.getAttribute('href'),
    );

    expect(enlaces).toEqual(['/login', '/registro']);
  });

  /** Con sesión se ofrece ir al panel del rol. */
  it('ofrece ir al panel con sesión', async () => {
    TestBed.inject(SesionService).iniciar(loginDePrueba('ESTUDIANTE'));
    const enlace = (await dibujar()).querySelector('a');

    expect(enlace?.getAttribute('href')).toBe('/estudiante/plan');
  });
});
