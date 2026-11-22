import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuditoriaDto } from '../../model/auditoria.model';
import { AuditoriaComponent, aFiltroApi, textoUsuario } from './auditoria.component';

/** Ruta de la auditoría. */
const URL = `${environment.apiUrl}/api/admin/auditoria`;

/** Registros de ejemplo: una inscripción exitosa y un login fallido anónimo. */
const REGISTROS: AuditoriaDto[] = [
  {
    id: 2,
    usuarioId: 3,
    usuarioCorreo: 'ana@example.com',
    accion: 'INSCRIPCION',
    fechaHora: '2026-11-20T15:00:00Z',
    entidad: 'INSCRIPCION',
    idRecurso: '7',
    resultado: 'EXITOSO',
  },
  {
    id: 1,
    usuarioId: null,
    accion: 'INICIO_SESION',
    fechaHora: '2026-11-20T14:00:00Z',
    entidad: 'USUARIO',
    resultado: 'FALLIDO',
    informacionAdicional: 'Credenciales inválidas',
  },
];

/**
 * Página de respuesta con los registros de ejemplo.
 *
 * @param pagina número de página
 * @return cuerpo de la respuesta
 */
const paginaDe = (pagina: number): object => ({
  contenido: REGISTROS,
  pagina,
  tamano: 20,
  totalElementos: 45,
  totalPaginas: 3,
});

/** Pruebas de las funciones de apoyo. */
describe('auditoría: funciones de apoyo', () => {
  /** El usuario se muestra con correo e id, solo id o como anónimo. */
  it('arma el texto del usuario', () => {
    expect(textoUsuario(REGISTROS[0])).toBe('ana@example.com (id 3)');
    expect(textoUsuario(REGISTROS[1])).toBe('Anónimo');
    expect(textoUsuario({ ...REGISTROS[0], usuarioCorreo: null })).toBe('id 3');
  });

  /** Las fechas locales pasan a instantes con zona y las vacías a nulo. */
  it('convierte los filtros del formulario', () => {
    const filtro = aFiltroApi({
      usuarioId: 3,
      accion: null,
      resultado: 'FALLIDO',
      desde: '2026-11-20T08:00',
      hasta: '',
    });
    expect(filtro.desde).toMatch(/Z$/);
    expect(filtro.hasta).toBeNull();
    expect(filtro.resultado).toBe('FALLIDO');
  });
});

/** Pruebas de la pantalla de auditoría. */
describe('AuditoriaComponent', () => {
  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<AuditoriaComponent>;

  /** Raíz del componente. */
  let html: HTMLElement;

  /** Dibuja la pantalla y responde la primera consulta. */
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [AuditoriaComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AuditoriaComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    const inicial = backend.expectOne((r) => r.url === URL);
    expect(inicial.request.params.keys().sort()).toEqual(['pagina', 'tamano']);
    inicial.flush(paginaDe(0));
    await fixture.whenStable();
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Muestra los registros con su acción legible y la etiqueta del resultado. */
  it('lista los registros', () => {
    const filas = html.querySelectorAll('tbody tr');
    expect(filas).toHaveLength(2);
    expect(filas[0].textContent).toContain('Inscripción');
    expect(filas[0].textContent).toContain('Exitoso');
    expect(filas[1].textContent).toContain('Anónimo');
    expect(filas[1].textContent).toContain('Fallido');
    expect(html.textContent).not.toContain('Eliminar');
    expect(html.textContent).toContain('45');
  });

  /** Al filtrar envía solo los filtros elegidos, desde la primera página. */
  it('aplica los filtros', async () => {
    const select = html.querySelector<HTMLSelectElement>('#filtro-resultado');
    if (!select) {
      throw new Error('No existe el filtro de resultado');
    }
    select.selectedIndex = 2;
    select.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    html.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();

    const peticion = backend.expectOne((r) => r.url === URL);
    expect(peticion.request.params.get('resultado')).toBe('FALLIDO');
    expect(peticion.request.params.get('pagina')).toBe('0');
    expect(peticion.request.params.has('accion')).toBe(false);
    peticion.flush(paginaDe(0));
  });

  /** Un rango con "desde" posterior a "hasta" no se consulta. */
  it('rechaza un rango invertido', async () => {
    for (const [id, valor] of [
      ['#filtro-desde', '2026-11-21T10:00'],
      ['#filtro-hasta', '2026-11-20T10:00'],
    ]) {
      const campo = html.querySelector<HTMLInputElement>(id);
      if (!campo) {
        throw new Error(`No existe ${id}`);
      }
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    }
    await fixture.whenStable();
    html.querySelector<HTMLButtonElement>('button[type="submit"]')?.click();
    await fixture.whenStable();
    expect(html.textContent).toContain('debe ser anterior');
  });
});
