import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { GrupoDto } from '../../model/grupo.model';
import { InscripcionDto } from '../../model/inscripcion.model';
import { GrupoService } from '../../service/grupo.service';
import { InscripcionService } from '../../service/inscripcion.service';
import { ToastService } from '../../service/toast.service';
import { mensajesDe } from '../../util/errores.util';
import { formatearHorarios } from '../../util/horario.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { ConfirmacionComponent } from '../confirmacion/confirmacion.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/** Máximo de créditos por periodo; solo se muestra, la regla la aplica el backend. */
export const MAXIMO_CREDITOS = 12;

/**
 * Créditos que el estudiante está cursando en el periodo.
 *
 * @param inscripciones inscripciones del periodo activo
 * @return suma de créditos de las inscripciones en curso
 */
export const creditosCursando = (inscripciones: InscripcionDto[]): number =>
  inscripciones
    .filter((i) => i.estado === 'CURSANDO')
    .reduce((total, i) => total + i.grupo.asignatura.creditos, 0);

/**
 * Indica si un grupo coincide con el texto buscado (código, nombre de la asignatura o profesor).
 *
 * @param grupo grupo de la oferta
 * @param texto texto buscado, ya en minúsculas
 * @return {@code true} si coincide o el texto está vacío
 */
const coincide = (grupo: GrupoDto, texto: string): boolean =>
  !texto ||
  [grupo.asignatura.codigo, grupo.asignatura.nombre, grupo.profesor.nombreCompleto].some((campo) =>
    campo.toLowerCase().includes(texto),
  );

/**
 * Oferta del periodo activo e inscripción (`/estudiante/oferta`). El frontend no decide si el
 * estudiante puede inscribirse: envía la solicitud y muestra todos los motivos que devuelva el backend.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-oferta',
  /** Dependencias de la plantilla. */
  imports: [FormsModule, TablaComponent, ConfirmacionComponent, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './oferta.component.html',
})
export class OfertaComponent implements OnInit {
  /** Servicio de grupos. */
  private readonly grupos = inject(GrupoService);

  /** Servicio de inscripciones. */
  private readonly inscripciones = inject(InscripcionService);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Máximo de créditos, para la plantilla. */
  protected readonly maximoCreditos = MAXIMO_CREDITOS;

  /** Columnas de la tabla. */
  protected readonly columnas: ColumnaTabla<GrupoDto>[] = [
    { titulo: 'Asignatura', valor: (g) => `${g.asignatura.codigo} · ${g.asignatura.nombre}` },
    { titulo: 'Créditos', valor: (g) => g.asignatura.creditos, clase: 'text-center' },
    { clave: 'numero', titulo: 'Grupo', clase: 'text-center' },
    { titulo: 'Profesor', valor: (g) => g.profesor.nombreCompleto },
    { titulo: 'Horario', valor: (g) => formatearHorarios(g.horarios), clase: 'small' },
    { titulo: 'Salón', valor: (g) => `${g.bloque}-${g.salon}` },
    { titulo: 'Cupos', valor: (g) => `${g.cuposDisponibles}/${g.cupoMaximo}`, clase: 'text-center' },
  ];

  /** Grupos ofertados en el periodo activo. */
  protected readonly oferta = signal<GrupoDto[]>([]);

  /** Inscripciones del estudiante en el periodo activo. */
  protected readonly mias = signal<InscripcionDto[]>([]);

  /** Texto de búsqueda. */
  protected readonly busqueda = signal('');

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Motivos del último rechazo, o errores de carga. */
  protected readonly errores = signal<string[]>([]);

  /** Grupo que espera confirmación. */
  protected readonly porInscribir = signal<GrupoDto | null>(null);

  /** Indica que se está enviando la inscripción. */
  protected readonly inscribiendo = signal(false);

  /** Código del periodo activo, tomado de la oferta. */
  protected readonly periodo = computed(() => this.oferta()[0]?.periodo.codigo ?? null);

  /** Créditos en curso. */
  protected readonly creditos = computed(() => creditosCursando(this.mias()));

  /** Asignaturas que el estudiante ya está cursando en el periodo. */
  private readonly asignaturasCursando = computed(
    () =>
      new Set(
        this.mias()
          .filter((i) => i.estado === 'CURSANDO')
          .map((i) => i.grupo.asignatura.id),
      ),
  );

  /** Grupos visibles según la búsqueda, por código de asignatura y número. */
  protected readonly visibles = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    return this.oferta()
      .filter((g) => coincide(g, texto))
      .sort((a, b) => a.asignatura.codigo.localeCompare(b.asignatura.codigo) || a.numero - b.numero);
  });

  /** Mensaje del cuadro de confirmación. */
  protected readonly mensajeConfirmacion = computed(() => {
    const grupo = this.porInscribir();
    return grupo
      ? `Vas a inscribir ${grupo.asignatura.nombre} (${grupo.asignatura.creditos} créditos), grupo ${grupo.numero}, ` +
          `con ${grupo.profesor.nombreCompleto}. Una inscripción confirmada no se puede cancelar.`
      : '';
  });

  /** Carga la oferta y las inscripciones al abrir la pantalla. */
  ngOnInit(): void {
    this.cargar();
  }

  /**
   * Indica si el estudiante ya cursa la asignatura de un grupo.
   *
   * @param grupo grupo de la oferta
   * @return {@code true} si ya la está cursando
   */
  protected yaCursa(grupo: GrupoDto): boolean {
    return this.asignaturasCursando().has(grupo.asignatura.id);
  }

  /**
   * Pide confirmación antes de inscribir.
   *
   * @param grupo grupo elegido
   */
  protected pedirConfirmacion(grupo: GrupoDto): void {
    this.errores.set([]);
    this.porInscribir.set(grupo);
  }

  /** Envía la inscripción confirmada; si el backend la rechaza, muestra todos los motivos juntos. */
  protected inscribir(): void {
    const grupo = this.porInscribir();
    if (!grupo) {
      return;
    }
    this.inscribiendo.set(true);
    this.inscripciones.inscribir(grupo.id).subscribe({
      next: () => {
        this.inscribiendo.set(false);
        this.porInscribir.set(null);
        this.avisos.exito(`Quedaste inscrito en ${grupo.asignatura.nombre}, grupo ${grupo.numero}.`);
        this.cargar();
      },
      error: (error: unknown) => {
        this.inscribiendo.set(false);
        this.porInscribir.set(null);
        this.errores.set([
          `No se pudo inscribir ${grupo.asignatura.nombre}:`,
          ...mensajesDe(error).map((motivo) => `• ${motivo}`),
        ]);
      },
    });
  }

  /** Trae la oferta y las inscripciones del periodo activo en paralelo. */
  private cargar(): void {
    this.cargando.set(true);
    forkJoin({ oferta: this.grupos.oferta(), mias: this.inscripciones.mias() }).subscribe({
      next: ({ oferta, mias }) => {
        this.oferta.set(oferta);
        this.mias.set(mias);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
