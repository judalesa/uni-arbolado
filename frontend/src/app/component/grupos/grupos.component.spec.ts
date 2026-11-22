import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { PERIODOS_PRUEBA, grupoDePrueba } from '../../util/datos-prueba.util';
import { GruposComponent, periodoInicial } from './grupos.component';

/** Pruebas de la elección del periodo inicial. */
describe('periodoInicial', () => {
  /** El periodo de la URL gana; si no hay, el activo; si no, el más reciente. */
  it('elige el periodo inicial', () => {
    expect(periodoInicial(PERIODOS_PRUEBA, 1)).toBe(1);
    expect(periodoInicial(PERIODOS_PRUEBA, null)).toBe(2);
    expect(periodoInicial([{ ...PERIODOS_PRUEBA[0] }], 99)).toBe(1);
    expect(periodoInicial([], null)).toBeNull();
  });
});

/** Pruebas del listado de grupos del administrador. */
describe('GruposComponent', () => {
  /** Ruta de los grupos. */
  const GRUPOS = `${environment.apiUrl}/api/admin/grupos`;

  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<GruposComponent>;

  /** Elemento de la pantalla. */
  let html: HTMLElement;

  /** Dibuja la pantalla y responde los periodos. */
  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [GruposComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GruposComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/periodos`).flush(PERIODOS_PRUEBA);
    await fixture.whenStable();
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Abre en el periodo activo y muestra cupos y horario de cada grupo. */
  it('lista los grupos del periodo activo', async () => {
    backend
      .expectOne((r) => r.url === GRUPOS && r.params.get('periodoId') === '2')
      .flush([grupoDePrueba({ inscritos: 12 })]);
    await fixture.whenStable();

    const fila = html.querySelector('tbody tr')?.textContent ?? '';
    expect(fila).toContain('DSI301');
    expect(fila).toContain('Marta Rojas');
    expect(fila).toContain('12/30');
    expect(fila).toContain('Lu 08:00–10:00');
  });

  /** Cambiar el periodo vuelve a pedir los grupos de ese periodo. */
  it('recarga al cambiar de periodo', async () => {
    backend.expectOne((r) => r.url === GRUPOS).flush([]);
    await fixture.whenStable();

    const filtro = html.querySelector<HTMLSelectElement>('#filtro-periodo');
    if (!filtro) {
      throw new Error('No existe el filtro de periodo');
    }
    filtro.selectedIndex = 1;
    filtro.dispatchEvent(new Event('change'));
    backend.expectOne((r) => r.url === GRUPOS && r.params.get('periodoId') === '1').flush([]);
    await fixture.whenStable();
    expect(html.textContent).toContain('No hay grupos en este periodo.');
  });
});
