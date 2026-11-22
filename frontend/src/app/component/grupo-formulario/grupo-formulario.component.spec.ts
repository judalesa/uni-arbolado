import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { GrupoDto } from '../../model/grupo.model';
import { PERIODOS_PRUEBA, grupoDePrueba } from '../../util/datos-prueba.util';
import { errorDePrueba } from '../../util/pruebas.util';
import { GrupoFormularioComponent } from './grupo-formulario.component';

/** Pruebas del formulario de grupo. */
describe('GrupoFormularioComponent', () => {
  /** Ruta de los grupos. */
  const GRUPOS = `${environment.apiUrl}/api/admin/grupos`;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<GrupoFormularioComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

  /**
   * Dibuja el formulario y responde las listas de los selectores.
   *
   * @param grupo grupo a modificar; sin él, el formulario es de creación
   */
  async function abrir(grupo?: GrupoDto): Promise<void> {
    TestBed.configureTestingModule({
      imports: [GrupoFormularioComponent],
      providers: [
        provideRouter([{ path: 'admin/grupos', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(grupo ? { id: String(grupo.id) } : {}),
              queryParamMap: convertToParamMap({}),
            },
          },
        },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GrupoFormularioComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/asignaturas`).flush([
      { id: 4, codigo: 'DSI301', nombre: 'Desarrollo de Sistemas 3', semestre: 6, creditos: 4, prerrequisitos: [] },
    ]);
    backend.expectOne(`${environment.apiUrl}/api/periodos`).flush(PERIODOS_PRUEBA);
    backend.expectOne((r) => r.url === `${environment.apiUrl}/api/admin/profesores`).flush({
      contenido: [
        { id: 5, documento: '1012345678', nombreCompleto: 'Marta Rojas', correo: 'm@example.com', rol: 'PROFESOR', estado: 'ACTIVO' },
      ],
      pagina: 0,
      tamano: 100,
      totalElementos: 1,
      totalPaginas: 1,
    });
    if (grupo) {
      backend.expectOne(`${GRUPOS}/${grupo.id}`).flush(grupo);
    }
    await fixture.whenStable();
  }

  /**
   * Elige una opción de un selector por su posición.
   *
   * @param id id del selector
   * @param indice posición de la opción
   */
  function elegir(id: string, indice: number): void {
    const selector = html.querySelector<HTMLSelectElement>(`#${id}`);
    if (!selector) {
      throw new Error(`No existe el selector ${id}`);
    }
    selector.selectedIndex = indice;
    selector.dispatchEvent(new Event('change'));
  }

  /**
   * Escribe en un campo por su id.
   *
   * @param id id del campo
   * @param valor texto a escribir
   */
  function escribirEn(id: string, valor: string): void {
    const campo = html.querySelector<HTMLInputElement>(`#${id}`);
    if (!campo) {
      throw new Error(`No existe el campo ${id}`);
    }
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  /** Llena los datos del grupo y una franja del lunes de 8 a 10. */
  function llenar(): void {
    elegir('campo-asignaturaId', 1);
    elegir('campo-profesorId', 1);
    escribirEn('campo-bloque', 'B');
    escribirEn('campo-salon', '301');
    elegir('dia-0', 1);
    escribirEn('inicio-0', '08:00');
    escribirEn('fin-0', '10:00');
  }

  /** Envía el formulario. */
  async function guardar(): Promise<void> {
    html.querySelector('form')?.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Al crear propone el periodo activo y envía el cuerpo del contrato. */
  it('crea un grupo', async () => {
    await abrir();
    llenar();
    await guardar();

    const peticion = backend.expectOne({ method: 'POST', url: GRUPOS });
    expect(peticion.request.body).toEqual({
      asignaturaId: 4,
      periodoId: 2,
      profesorId: 5,
      bloque: 'B',
      salon: '301',
      horarios: [{ diaSemana: 'LUNES', horaInicio: '08:00', horaFin: '10:00' }],
    });
    peticion.flush(grupoDePrueba(), { status: 201, statusText: 'Created' });
  });

  /** Dos franjas que se traslapan se señalan sin llamar al backend. */
  it('no envía horarios traslapados', async () => {
    await abrir();
    llenar();
    html.querySelector<HTMLButtonElement>('.btn-outline-success')?.click();
    fixture.detectChanges();
    elegir('dia-1', 1);
    escribirEn('inicio-1', '09:00');
    escribirEn('fin-1', '11:00');
    await guardar();

    backend.expectNone(GRUPOS);
    expect(html.querySelector('.alert-danger')?.textContent).toContain('se traslapan el lunes');
  });

  /** Con inscritos, asignatura, periodo y horarios quedan bloqueados y se avisa por qué. */
  it('bloquea la estructura de un grupo con inscritos', async () => {
    await abrir(grupoDePrueba({ inscritos: 3 }));

    expect(html.querySelector('.alert-info')?.textContent).toContain('3 estudiantes inscritos');
    expect(html.querySelector<HTMLSelectElement>('#campo-asignaturaId')?.disabled).toBe(true);
    expect(html.querySelector<HTMLSelectElement>('#dia-0')?.disabled).toBe(true);
    expect(html.querySelector<HTMLSelectElement>('#campo-profesorId')?.disabled).toBe(false);

    await guardar();
    const peticion = backend.expectOne({ method: 'PUT', url: `${GRUPOS}/10` });
    expect(peticion.request.body.asignaturaId).toBe(4);
    expect(peticion.request.body.horarios).toHaveLength(1);
    peticion.flush(grupoDePrueba({ inscritos: 3 }));
  });

  /** Un rechazo del backend se muestra en el cuadro de errores. */
  it('muestra el rechazo del backend', async () => {
    await abrir();
    llenar();
    await guardar();

    backend
      .expectOne({ method: 'POST', url: GRUPOS })
      .flush(errorDePrueba(422, 'REGLA_NEGOCIO', 'La persona elegida no tiene el rol de profesor'), {
        status: 422,
        statusText: 'Unprocessable Entity',
      });
    await fixture.whenStable();
    expect(html.querySelector('.alert-danger')?.textContent).toContain('no tiene el rol de profesor');
  });
});
