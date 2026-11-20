import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { SesionService } from '../../service/sesion.service';
import { loginDePrueba } from '../../util/pruebas.util';
import { NavbarComponent } from './navbar.component';

/** Pruebas de la barra de navegación. */
describe('NavbarComponent', () => {
  /** Prepara el entorno con un enrutador vacío y la sesión limpia. */
  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  /**
   * Dibuja la barra y devuelve su elemento.
   *
   * @return el elemento raíz de la barra
   */
  async function dibujar(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(NavbarComponent);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  /** Sin sesión solo se ve la marca: ni menú ni botón de salir. */
  it('muestra solo la marca sin sesión', async () => {
    const html = await dibujar();

    expect(html.querySelector('.navbar-brand')?.textContent).toContain('Universidad El Arbolado');
    expect(html.querySelector('.nav-link')).toBeNull();
  });

  /** Un administrador ve sus opciones y no las del estudiante. */
  it('muestra el menú del rol', async () => {
    TestBed.inject(SesionService).iniciar(loginDePrueba('ADMINISTRADOR'));
    const texto = (await dibujar()).textContent;

    expect(texto).toContain('Periodos');
    expect(texto).toContain('Auditoría');
    expect(texto).not.toContain('Oferta e inscripción');
  });

  /** Al salir se borra la sesión. */
  it('cierra la sesión al pulsar Salir', async () => {
    const sesion = TestBed.inject(SesionService);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    sesion.iniciar(loginDePrueba('PROFESOR'));
    const html = await dibujar();
    const salir = Array.from(html.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Salir'),
    );
    salir?.click();

    expect(sesion.estaVigente()).toBe(false);
  });
});
