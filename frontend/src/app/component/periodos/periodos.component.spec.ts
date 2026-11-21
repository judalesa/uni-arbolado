import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { PeriodoDto } from '../../model/periodo.model';
import { enviarFormulario, errorDePrueba, escribir } from '../../util/pruebas.util';
import { PeriodosComponent } from './periodos.component';

/** Pruebas de la pantalla de periodos. */
describe('PeriodosComponent', () => {
  /** Base de las rutas de periodos. */
  const BASE = `${environment.apiUrl}/api/periodos`;

  /** Periodos de ejemplo, en desorden. */
  const PERIODOS: PeriodoDto[] = [
    { id: 1, codigo: '2026-1', fechaInicio: '2026-01-19', fechaFin: '2026-06-06', activo: true },
    { id: 2, codigo: '2026-2', fechaInicio: '2026-07-20', fechaFin: '2026-12-05', activo: false },
  ];

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<PeriodosComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

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

  /** Dibuja la pantalla y responde el listado inicial. */
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [PeriodosComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PeriodosComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(BASE).flush(PERIODOS);
    await fixture.whenStable();
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** El listado va del más reciente al más antiguo, con fechas legibles. */
  it('lista los periodos ordenados', () => {
    const filas = html.querySelectorAll('tbody tr');
    expect(filas[0].textContent).toContain('2026-2');
    expect(filas[0].textContent).toContain('20/07/2026');
    expect(filas[1].textContent).toContain('2026-1');
  });

  /** Crear envía el cuerpo del contrato y recarga el listado. */
  it('crea un periodo', async () => {
    await pulsar('Nuevo periodo');
    escribir(html, 'codigo', '2027-1');
    escribir(html, 'fechaInicio', '2027-01-18');
    escribir(html, 'fechaFin', '2027-06-05');
    enviarFormulario(html);

    const peticion = backend.expectOne({ method: 'POST', url: BASE });
    expect(peticion.request.body).toEqual({
      codigo: '2027-1',
      fechaInicio: '2027-01-18',
      fechaFin: '2027-06-05',
    });
    peticion.flush({ ...PERIODOS[1], id: 3, codigo: '2027-1' }, { status: 201, statusText: 'Created' });
    backend.expectOne(BASE).flush(PERIODOS);
  });

  /** Un código repetido se marca en el campo del código. */
  it('marca el código duplicado en su campo', async () => {
    await pulsar('Nuevo periodo');
    escribir(html, 'codigo', '2026-1');
    escribir(html, 'fechaInicio', '2026-01-19');
    escribir(html, 'fechaFin', '2026-06-06');
    enviarFormulario(html);

    backend
      .expectOne({ method: 'POST', url: BASE })
      .flush(errorDePrueba(409, 'PERIODO_DUPLICADO', 'Ya existe un periodo con ese código'), {
        status: 409,
        statusText: 'Conflict',
      });
    await fixture.whenStable();

    expect(html.querySelector('#campo-codigo')?.classList).toContain('is-invalid');
    expect(html.textContent).toContain('Ya existe un periodo con ese código');
  });

  /** Modificar llena el formulario con los datos actuales y envía un PUT. */
  it('modifica un periodo', async () => {
    await pulsar('Modificar');
    expect(html.querySelector<HTMLInputElement>('#campo-codigo')?.value).toBe('2026-2');
    enviarFormulario(html);

    backend.expectOne({ method: 'PUT', url: `${BASE}/2` }).flush(PERIODOS[1]);
    backend.expectOne(BASE).flush(PERIODOS);
  });

  /** Activar pide confirmación nombrando el periodo que se desactiva. */
  it('activa después de confirmar', async () => {
    await pulsar('Activar');
    expect(html.querySelector('.modal')?.textContent).toContain('se desactivará 2026-1');
    backend.expectNone(`${BASE}/2/activar`);

    const confirmar = html.querySelector<HTMLButtonElement>('.modal-footer .btn-success');
    confirmar?.click();
    backend.expectOne({ method: 'POST', url: `${BASE}/2/activar` }).flush({ ...PERIODOS[1], activo: true });
    backend.expectOne(BASE).flush(PERIODOS);
  });

  /** Si el periodo actual tiene notas pendientes, se muestra el motivo del backend. */
  it('muestra el rechazo por notas pendientes', async () => {
    await pulsar('Activar');
    html.querySelector<HTMLButtonElement>('.modal-footer .btn-success')?.click();
    backend.expectOne(`${BASE}/2/activar`).flush(
      {
        ...errorDePrueba(422, 'PERIODO_CON_NOTAS_PENDIENTES', 'No se puede activar'),
        motivos: [{ codigo: 'NOTAS_PENDIENTES', mensaje: 'El periodo 2026-1 tiene 3 inscripciones sin nota' }],
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    await fixture.whenStable();

    expect(html.querySelector('.modal')).toBeNull();
    expect(html.querySelector('.alert-danger')?.textContent).toContain('3 inscripciones sin nota');
  });
});
