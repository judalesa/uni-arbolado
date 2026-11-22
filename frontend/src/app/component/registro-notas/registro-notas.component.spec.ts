import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { InscripcionDto } from '../../model/inscripcion.model';
import { ToastService } from '../../service/toast.service';
import { PERIODOS_PRUEBA, grupoDePrueba } from '../../util/datos-prueba.util';
import { RegistroNotasComponent } from './registro-notas.component';

/** Base de las rutas del profesor. */
const BASE = `${environment.apiUrl}/api/profesor`;

/** Inscripción con los cortes 1 y 2 registrados y el 3 pendiente. */
const ANA: InscripcionDto = {
  id: 7,
  estudiante: { id: 3, documento: '1000000003', nombreCompleto: 'Ana Pérez' },
  grupo: grupoDePrueba(),
  estado: 'CURSANDO',
  notaCorte1: 3.5,
  notaCorte2: 4,
  notaCorte3: null,
};

/** Pruebas del registro de notas. */
describe('RegistroNotasComponent', () => {
  /** Controlador de las peticiones simuladas. */
  let backend: HttpTestingController;

  /** Componente dibujado. */
  let fixture: ComponentFixture<RegistroNotasComponent>;

  /** Raíz del componente. */
  let html: HTMLElement;

  /**
   * Abre la pantalla del grupo 10 y responde con las inscripciones dadas.
   *
   * @param inscripciones respuesta del backend
   */
  const abrir = async (inscripciones: InscripcionDto[]): Promise<void> => {
    fixture = TestBed.createComponent(RegistroNotasComponent);
    html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${BASE}/grupos/10/estudiantes`).flush(inscripciones);
    await fixture.whenStable();
  };

  /**
   * Escribe una nota en el corte 3 de Ana y pulsa Guardar.
   *
   * @param valor texto escrito
   */
  const guardarCorte3 = async (valor: string): Promise<void> => {
    const campo = html.querySelector<HTMLInputElement>('#nota-7-3');
    if (!campo) {
      throw new Error('No existe el campo del corte 3');
    }
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    campo.closest('div')?.querySelector('button')?.click();
    await fixture.whenStable();
  };

  /** Prepara el módulo con el grupo 10 en la URL. */
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegistroNotasComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '10' }) } },
        },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  /** Verifica que no queden peticiones sin atender. */
  afterEach(() => backend.verify());

  /** Los cortes con nota aparecen bloqueados y solo el pendiente tiene campo. */
  it('bloquea los cortes que ya tienen nota', async () => {
    await abrir([ANA]);
    const fila = html.querySelector('tbody tr');
    expect(html.querySelector('h1')?.textContent).toContain('Desarrollo de Sistemas 3 · Grupo 1');
    expect(fila?.textContent).toContain('3.5');
    expect(fila?.querySelectorAll('.fa-lock')).toHaveLength(2);
    expect(fila?.querySelectorAll('input')).toHaveLength(1);
    expect(fila?.querySelector('a')?.getAttribute('href')).toBe(
      '/profesor/estudiantes/3/historial',
    );
  });

  /** Pide confirmación, envía la nota y muestra la definitiva que devuelve el backend. */
  it('registra la nota después de confirmar', async () => {
    const aviso = vi.spyOn(TestBed.inject(ToastService), 'exito');
    await abrir([ANA]);
    await guardarCorte3('3.8');
    expect(html.querySelector('.modal-body')?.textContent).toContain('no se podrá corregir');

    html.querySelector<HTMLButtonElement>('.modal-footer .btn-success')?.click();
    const peticion = backend.expectOne(`${BASE}/inscripciones/7/notas`);
    expect(peticion.request.body).toEqual({ corte: 3, valor: 3.8 });
    peticion.flush({ ...ANA, notaCorte3: 3.8, notaDefinitiva: 3.8, estado: 'APROBADA' });
    await fixture.whenStable();

    expect(html.querySelectorAll('tbody .fa-lock')).toHaveLength(3);
    expect(html.querySelector('tbody tr')?.textContent).toContain('Aprobada');
    expect(aviso).toHaveBeenCalledWith('Nota 3.8 registrada en el corte 3. Definitiva: 3.8.');
  });

  /** Un valor inválido no abre la confirmación ni llega al backend. */
  it('rechaza una nota inválida', async () => {
    await abrir([ANA]);
    await guardarCorte3('5.5');
    expect(html.querySelector('.modal')).toBeNull();
    expect(html.textContent).toContain('de 0.0 a 5.0');
  });

  /** Si otro registro ganó (409), muestra el mensaje y recarga la lista. */
  it('muestra el conflicto y recarga', async () => {
    await abrir([ANA]);
    await guardarCorte3('3.0');
    html.querySelector<HTMLButtonElement>('.modal-footer .btn-success')?.click();
    backend
      .expectOne(`${BASE}/inscripciones/7/notas`)
      .flush(
        { codigo: 'NOTA_YA_REGISTRADA', mensaje: 'La nota de ese corte ya fue registrada.' },
        { status: 409, statusText: 'Conflict' },
      );
    backend.expectOne(`${BASE}/grupos/10/estudiantes`).flush([{ ...ANA, notaCorte3: 2 }]);
    await fixture.whenStable();
    expect(html.textContent).toContain('ya fue registrada');
    expect(html.querySelectorAll('tbody .fa-lock')).toHaveLength(3);
  });

  /** En un periodo inactivo no hay campos para escribir. */
  it('solo consulta en un periodo inactivo', async () => {
    await abrir([{ ...ANA, grupo: grupoDePrueba({ periodo: PERIODOS_PRUEBA[0] }) }]);
    expect(html.querySelector('tbody input')).toBeNull();
    expect(html.textContent).toContain('no está activo');
  });
});
