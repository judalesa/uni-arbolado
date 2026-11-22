import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { InscripcionDto } from '../../model/inscripcion.model';
import { grupoDePrueba } from '../../util/datos-prueba.util';
import { errorDePrueba } from '../../util/pruebas.util';
import { OfertaComponent, creditosCursando } from './oferta.component';

/** Grupo de otra asignatura, que el estudiante ya cursa. */
const CURSADO = grupoDePrueba({
  id: 11,
  asignatura: { id: 7, codigo: 'BDA201', nombre: 'Bases de Datos 2', creditos: 3 },
});

/** Grupo sin cupos. */
const LLENO = grupoDePrueba({
  id: 12,
  asignatura: { id: 8, codigo: 'RED301', nombre: 'Redes', creditos: 3 },
  cuposDisponibles: 0,
  inscritos: 30,
});

/** Inscripción vigente del estudiante. */
const MIA: InscripcionDto = { id: 1, grupo: CURSADO, estado: 'CURSANDO' };

/** Pruebas de la suma de créditos. */
describe('creditosCursando', () => {
  /** Solo cuentan las inscripciones en curso. */
  it('suma solo lo que se está cursando', () => {
    expect(creditosCursando([MIA, { ...MIA, id: 2, estado: 'APROBADA' }])).toBe(3);
  });
});

/** Pruebas de la oferta e inscripción. */
describe('OfertaComponent', () => {
  /** Ruta de inscripción. */
  const INSCRIPCIONES = `${environment.apiUrl}/api/inscripciones`;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<OfertaComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

  /** Responde la oferta y las inscripciones del estudiante. */
  function responderCarga(): void {
    backend.expectOne(`${environment.apiUrl}/api/grupos/oferta`).flush([LLENO, grupoDePrueba(), CURSADO]);
    backend.expectOne((r) => r.url === `${environment.apiUrl}/api/estudiante/inscripciones`).flush([MIA]);
  }

  /**
   * Fila de la tabla que contiene un texto.
   *
   * @param texto texto a buscar
   * @return la fila
   */
  function fila(texto: string): HTMLElement {
    const encontrada = [...html.querySelectorAll<HTMLElement>('tbody tr')].find((f) =>
      f.textContent?.includes(texto),
    );
    if (!encontrada) {
      throw new Error(`No hay fila con ${texto}`);
    }
    return encontrada;
  }

  /** Pide inscribir el grupo de DSI301 y confirma. */
  async function inscribirDsi(): Promise<void> {
    fila('DSI301').querySelector<HTMLButtonElement>('button')?.click();
    await fixture.whenStable();
    html.querySelector<HTMLButtonElement>('.modal-footer .btn-success')?.click();
  }

  /** Dibuja la pantalla con la carga inicial. */
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [OfertaComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OfertaComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    responderCarga();
    await fixture.whenStable();
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Muestra el periodo, los créditos, el grupo lleno deshabilitado y la asignatura ya inscrita. */
  it('muestra la oferta con su estado', () => {
    expect(html.querySelector('h1')?.textContent).toContain('2026-2');
    expect(html.textContent).toContain('Créditos cursando: 3 / 12');
    expect(fila('RED301').querySelector('button')?.disabled).toBe(true);
    expect(fila('BDA201').textContent).toContain('Inscrita');
    expect(fila('DSI301').querySelector('button')?.disabled).toBe(false);
  });

  /** La confirmación advierte que no se puede cancelar y, al aceptar, inscribe y recarga. */
  it('inscribe después de confirmar', async () => {
    fila('DSI301').querySelector<HTMLButtonElement>('button')?.click();
    await fixture.whenStable();
    expect(html.querySelector('.modal')?.textContent).toContain('no se puede cancelar');
    backend.expectNone(INSCRIPCIONES);

    html.querySelector<HTMLButtonElement>('.modal-footer .btn-success')?.click();
    const peticion = backend.expectOne({ method: 'POST', url: INSCRIPCIONES });
    expect(peticion.request.body).toEqual({ grupoId: 10 });
    peticion.flush({ id: 2, grupo: grupoDePrueba(), estado: 'CURSANDO' }, { status: 201, statusText: 'Created' });
    responderCarga();
  });

  /** Un rechazo muestra todos los motivos a la vez. */
  it('muestra todos los motivos del rechazo', async () => {
    await inscribirDsi();
    backend.expectOne(INSCRIPCIONES).flush(
      {
        ...errorDePrueba(422, 'REGLA_NEGOCIO', 'No se pudo realizar la inscripción'),
        motivos: [
          { codigo: 'PREREQUISITO_NO_CUMPLIDO', mensaje: 'Falta aprobar Bases de Datos 1' },
          { codigo: 'CREDITOS_EXCEDIDOS', mensaje: 'Superarías el máximo de 12 créditos' },
        ],
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
    await fixture.whenStable();

    const alerta = html.querySelector('.alert-danger')?.textContent ?? '';
    expect(alerta).toContain('No se pudo inscribir Desarrollo de Sistemas 3');
    expect(alerta).toContain('Falta aprobar Bases de Datos 1');
    expect(alerta).toContain('Superarías el máximo de 12 créditos');
    expect(html.querySelector('.modal')).toBeNull();
  });

  /** La búsqueda filtra por código, nombre o profesor. */
  it('filtra la oferta', async () => {
    const busqueda = html.querySelector<HTMLInputElement>('#busqueda-oferta');
    if (!busqueda) {
      throw new Error('No existe la búsqueda');
    }
    busqueda.value = 'redes';
    busqueda.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(html.querySelectorAll('tbody tr')).toHaveLength(1);
  });
});
