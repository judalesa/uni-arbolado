import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { PeriodoDto, PeriodoRequest } from '../../model/periodo.model';
import { PeriodoService } from '../../service/periodo.service';
import { ToastService } from '../../service/toast.service';
import { formatearFecha } from '../../util/fechas.util';
import { erroresPorCampo, leerError, mensajesDe } from '../../util/errores.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { ConfirmacionComponent } from '../confirmacion/confirmacion.component';
import {
  CampoFormulario,
  FormularioComponent,
  ValoresFormulario,
} from '../formulario/formulario.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/** Pantalla de periodos del administrador (`/admin/periodos`): listar, crear, modificar y activar. */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-periodos',
  /** Dependencias de la plantilla. */
  imports: [TablaComponent, FormularioComponent, ConfirmacionComponent, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './periodos.component.html',
})
export class PeriodosComponent implements OnInit {
  /** Servicio de periodos. */
  private readonly servicio = inject(PeriodoService);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Columnas de la tabla. */
  protected readonly columnas: ColumnaTabla<PeriodoDto>[] = [
    { clave: 'codigo', titulo: 'Código' },
    { titulo: 'Inicio', valor: (p) => formatearFecha(p.fechaInicio) },
    { titulo: 'Fin', valor: (p) => formatearFecha(p.fechaFin) },
    { titulo: 'Estado', valor: (p) => (p.activo ? 'Activo' : 'Inactivo') },
  ];

  /** Campos del formulario; las reglas son las de `PeriodoRequest`. */
  protected readonly campos: CampoFormulario[] = [
    {
      nombre: 'codigo',
      etiqueta: 'Código',
      tipo: 'text',
      requerido: true,
      patron: '^[0-9]{4}-[0-9]$',
      mensajePatron: 'Usa el formato AAAA-N, por ejemplo 2026-2.',
      marcador: '2026-2',
    },
    { nombre: 'fechaInicio', etiqueta: 'Fecha de inicio', tipo: 'date', requerido: true },
    {
      nombre: 'fechaFin',
      etiqueta: 'Fecha de fin',
      tipo: 'date',
      requerido: true,
      ayuda: 'Debe ser posterior a la de inicio.',
    },
  ];

  /** Periodos, del más reciente al más antiguo. */
  protected readonly periodos = signal<PeriodoDto[]>([]);

  /** Indica que se está cargando el listado. */
  protected readonly cargando = signal(true);

  /** Formulario visible: `null` oculto, `'nuevo'` para crear o el periodo que se edita. */
  protected readonly edicion = signal<PeriodoDto | 'nuevo' | null>(null);

  /** Valores iniciales del formulario según lo que se edita. */
  protected readonly valoresIniciales = computed((): ValoresFormulario => {
    const actual = this.edicion();
    if (!actual || actual === 'nuevo') {
      return {};
    }
    return {
      codigo: actual.codigo,
      fechaInicio: actual.fechaInicio ?? '',
      fechaFin: actual.fechaFin ?? '',
    };
  });

  /** Título del formulario. */
  protected readonly tituloFormulario = computed(() => {
    const actual = this.edicion();
    return actual && actual !== 'nuevo' ? `Modificar el periodo ${actual.codigo}` : 'Nuevo periodo';
  });

  /** Indica que se está guardando el formulario. */
  protected readonly enviando = signal(false);

  /** Mensajes de error generales. */
  protected readonly errores = signal<string[]>([]);

  /** Errores por campo devueltos por el backend. */
  protected readonly erroresCampo = signal<Record<string, string>>({});

  /** Periodo que espera confirmación para activarse. */
  protected readonly porActivar = signal<PeriodoDto | null>(null);

  /** Indica que se está activando un periodo. */
  protected readonly activando = signal(false);

  /** Mensaje del cuadro de confirmación de activación. */
  protected readonly mensajeActivar = computed(() => {
    const periodo = this.porActivar();
    const vigente = this.periodos().find((p) => p.activo);
    if (!periodo) {
      return '';
    }
    return vigente
      ? `Se activará ${periodo.codigo} y se desactivará ${vigente.codigo}. Los estudiantes solo podrán inscribirse en grupos del periodo activo.`
      : `Se activará ${periodo.codigo}. Los estudiantes solo podrán inscribirse en grupos del periodo activo.`;
  });

  /** Carga el listado al abrir la pantalla. */
  ngOnInit(): void {
    this.cargar();
  }

  /** Muestra el formulario vacío para crear un periodo. */
  protected nuevo(): void {
    this.limpiarErrores();
    this.edicion.set('nuevo');
  }

  /**
   * Muestra el formulario con los datos de un periodo para modificarlo.
   *
   * @param periodo periodo a modificar
   */
  protected editar(periodo: PeriodoDto): void {
    this.limpiarErrores();
    this.edicion.set(periodo);
  }

  /** Oculta el formulario sin guardar. */
  protected cancelar(): void {
    this.limpiarErrores();
    this.edicion.set(null);
  }

  /**
   * Crea o modifica el periodo según lo que se esté editando.
   *
   * @param valores datos del formulario
   */
  protected guardar(valores: ValoresFormulario): void {
    const actual = this.edicion();
    const datos: PeriodoRequest = {
      codigo: String(valores['codigo']),
      fechaInicio: String(valores['fechaInicio']),
      fechaFin: String(valores['fechaFin']),
    };
    const peticion =
      actual && actual !== 'nuevo'
        ? this.servicio.modificar(actual.id, datos)
        : this.servicio.crear(datos);
    this.enviando.set(true);
    this.limpiarErrores();
    peticion.subscribe({
      next: (periodo) => {
        this.enviando.set(false);
        this.edicion.set(null);
        this.avisos.exito(
          actual === 'nuevo' ? `Periodo ${periodo.codigo} creado.` : `Periodo ${periodo.codigo} modificado.`,
        );
        this.cargar();
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        if (leerError(error)?.codigo === 'PERIODO_DUPLICADO') {
          this.erroresCampo.set({ codigo: leerError(error)?.mensaje ?? '' });
          return;
        }
        this.errores.set(mensajesDe(error));
        this.erroresCampo.set(erroresPorCampo(error));
      },
    });
  }

  /**
   * Pide confirmación antes de activar un periodo.
   *
   * @param periodo periodo a activar
   */
  protected pedirActivar(periodo: PeriodoDto): void {
    this.errores.set([]);
    this.porActivar.set(periodo);
  }

  /** Activa el periodo confirmado. Si el actual tiene notas pendientes, el backend lo rechaza (422). */
  protected activar(): void {
    const periodo = this.porActivar();
    if (!periodo) {
      return;
    }
    this.activando.set(true);
    this.servicio.activar(periodo.id).subscribe({
      next: (activo) => {
        this.activando.set(false);
        this.porActivar.set(null);
        this.avisos.exito(`El periodo ${activo.codigo} quedó activo.`);
        this.cargar();
      },
      error: (error: unknown) => {
        this.activando.set(false);
        this.porActivar.set(null);
        this.errores.set(mensajesDe(error));
      },
    });
  }

  /** Trae los periodos y los ordena del más reciente al más antiguo por código. */
  private cargar(): void {
    this.cargando.set(true);
    this.servicio.listar().subscribe({
      next: (lista) => {
        this.periodos.set([...lista].sort((a, b) => b.codigo.localeCompare(a.codigo)));
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }

  /** Borra los errores visibles. */
  private limpiarErrores(): void {
    this.errores.set([]);
    this.erroresCampo.set({});
  }
}
