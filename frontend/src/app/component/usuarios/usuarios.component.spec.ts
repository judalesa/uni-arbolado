import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { Pagina } from '../../model/pagina.model';
import { UsuarioDto } from '../../model/usuario.model';
import { enviarFormulario, errorDePrueba, escribir } from '../../util/pruebas.util';
import { UsuariosComponent } from './usuarios.component';

/**
 * Página de ejemplo con un usuario.
 *
 * @param totalPaginas total de páginas
 * @return página de usuarios
 */
const paginaDe = (totalPaginas = 1): Pagina<UsuarioDto> => ({
  contenido: [
    {
      id: 5,
      documento: '1012345678',
      nombreCompleto: 'Marta Rojas',
      correo: 'marta@example.com',
      rol: 'PROFESOR',
      estado: 'ACTIVO',
      fechaCreacion: '2026-11-20T15:00:00Z',
    },
  ],
  pagina: 0,
  tamano: 20,
  totalElementos: totalPaginas * 20,
  totalPaginas,
});

/** Pruebas del listado de usuarios del administrador. */
describe('UsuariosComponent', () => {
  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<UsuariosComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

  /**
   * Dibuja la pantalla para un rol.
   *
   * @param rol rol que indica la ruta
   */
  async function abrir(rol: 'ESTUDIANTE' | 'PROFESOR'): Promise<void> {
    TestBed.configureTestingModule({
      imports: [UsuariosComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { rol } } } },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UsuariosComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  /**
   * Pulsa el primer botón cuyo texto contiene el indicado.
   *
   * @param texto texto del botón
   */
  async function pulsar(texto: string): Promise<void> {
    const boton = [...html.querySelectorAll('button')].find((b) => b.textContent?.includes(texto));
    if (!boton) {
      throw new Error(`No existe el botón ${texto}`);
    }
    boton.click();
    await fixture.whenStable();
  }

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Estudiantes: lista su ruta y no ofrece creación. */
  it('lista estudiantes sin opción de crear', async () => {
    await abrir('ESTUDIANTE');
    backend.expectOne((r) => r.url.endsWith('/api/admin/estudiantes')).flush(paginaDe());
    await fixture.whenStable();

    expect(html.textContent).toContain('Marta Rojas');
    expect(html.textContent).not.toContain('Nuevo profesor');
  });

  /** La búsqueda vuelve a la primera página y se conserva al paginar. */
  it('busca y pagina con el mismo texto', async () => {
    await abrir('PROFESOR');
    backend.expectOne((r) => r.url.endsWith('/api/admin/profesores')).flush(paginaDe(3));
    await fixture.whenStable();

    const caja = html.querySelector<HTMLInputElement>('input[type=search]');
    caja!.value = 'rojas';
    caja!.dispatchEvent(new Event('input'));
    html.querySelector<HTMLFormElement>('form[role=search]')?.dispatchEvent(new Event('submit'));
    const busqueda = backend.expectOne((r) => r.params.get('q') === 'rojas');
    expect(busqueda.request.params.get('pagina')).toBe('0');
    busqueda.flush(paginaDe(3));
    await fixture.whenStable();

    await pulsar('Siguiente');
    const siguiente = backend.expectOne((r) => r.params.get('pagina') === '1');
    expect(siguiente.request.params.get('q')).toBe('rojas');
    siguiente.flush(paginaDe(3));
  });

  /** Crear un profesor envía el cuerpo del contrato y recarga el listado. */
  it('crea un profesor', async () => {
    await abrir('PROFESOR');
    backend.expectOne((r) => r.url.endsWith('/api/admin/profesores')).flush(paginaDe());
    await fixture.whenStable();

    await pulsar('Nuevo profesor');
    escribir(html, 'documento', '1098765432');
    escribir(html, 'nombreCompleto', 'Luis Pardo');
    escribir(html, 'correo', 'luis@example.com');
    escribir(html, 'contrasena', 'secreto123');
    enviarFormulario(html);

    const peticion = backend.expectOne({
      method: 'POST',
      url: `${environment.apiUrl}/api/admin/profesores`,
    });
    expect(peticion.request.body).toEqual({
      documento: '1098765432',
      nombreCompleto: 'Luis Pardo',
      correo: 'luis@example.com',
      contrasena: 'secreto123',
    });
    peticion.flush(paginaDe().contenido[0], { status: 201, statusText: 'Created' });
    backend.expectOne((r) => r.method === 'GET').flush(paginaDe());
  });

  /** Un correo repetido se marca en su campo. */
  it('marca el correo duplicado', async () => {
    await abrir('PROFESOR');
    backend.expectOne((r) => r.url.endsWith('/api/admin/profesores')).flush(paginaDe());
    await fixture.whenStable();

    await pulsar('Nuevo profesor');
    escribir(html, 'documento', '1098765432');
    escribir(html, 'nombreCompleto', 'Luis Pardo');
    escribir(html, 'correo', 'marta@example.com');
    escribir(html, 'contrasena', 'secreto123');
    enviarFormulario(html);

    backend
      .expectOne((r) => r.method === 'POST')
      .flush(errorDePrueba(409, 'CORREO_DUPLICADO', 'Ya existe una cuenta con ese correo'), {
        status: 409,
        statusText: 'Conflict',
      });
    await fixture.whenStable();

    expect(html.querySelector('#campo-correo')?.classList).toContain('is-invalid');
    expect(html.textContent).toContain('Ya existe una cuenta con ese correo');
  });
});
