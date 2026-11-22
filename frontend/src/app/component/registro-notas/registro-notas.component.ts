import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Corte, InscripcionDto } from '../../model/inscripcion.model';
import { ProfesorService } from '../../service/profesor.service';
import { ToastService } from '../../service/toast.service';
import { mensajesDe } from '../../util/errores.util';
import { formatearNota, leerNota } from '../../util/notas.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { ConfirmacionComponent } from '../confirmacion/confirmacion.component';
import { EtiquetaEstadoComponent } from '../etiqueta-estado/etiqueta-estado.component';

/** Datos de un corte para las columnas de la tabla. */
interface DatosCorte {
  /** Número del corte. */
  corte: Corte;
  /** Encabezado con su peso. */
  titulo: string;
  /** Campo de la inscripción que guarda la nota. */
  campo: 'notaCorte1' | 'notaCorte2' | 'notaCorte3';
}

/** Los tres cortes con su peso (30/30/40). */
export const CORTES: DatosCorte[] = [
  { corte: 1, titulo: 'Corte 1 (30 %)', campo: 'notaCorte1' },
  { corte: 2, titulo: 'Corte 2 (30 %)', campo: 'notaCorte2' },
  { corte: 3, titulo: 'Corte 3 (40 %)', campo: 'notaCorte3' },
];

/** Nota escrita que espera la confirmación del profesor. */
interface NotaPorRegistrar {
  /** Inscripción calificada. */
  inscripcion: InscripcionDto;
  /** Corte calificado. */
  corte: DatosCorte;
  /** Valor que se va a registrar. */
  valor: number;
}

/** Mensaje cuando el valor escrito no es una nota válida. */
const NOTA_INVALIDA = 'Escribe una nota de 0.0 a 5.0 con máximo un decimal.';

/**
 * Clave del borrador de una celda.
 *
 * @param inscripcion inscripción de la fila
 * @param corte corte de la columna
 * @return clave única de la celda
 */
const claveDe = (inscripcion: InscripcionDto, corte: DatosCorte): string =>
  `${inscripcion.id}-${corte.corte}`;

/**
 * Estudiantes de un grupo del profesor y registro de notas (`/profesor/grupos/:id`). Cada corte se
 * registra una sola vez: cuando ya tiene nota aparece bloqueado y antes de guardar se pide confirmación.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-registro-notas',
  /** Dependencias de la plantilla. */
  imports: [RouterLink, AlertaErroresComponent, ConfirmacionComponent, EtiquetaEstadoComponent],
  /** Plantilla HTML. */
  templateUrl: './registro-notas.component.html',
  /** Estilos propios de la tabla de notas. */
  styleUrl: './registro-notas.component.css',
})
export class RegistroNotasComponent implements OnInit {
  /** Consultas y registro de notas del profesor. */
  private readonly profesor = inject(ProfesorService);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Ruta actual, para leer el grupo. */
  private readonly ruta = inject(ActivatedRoute);

  /** Cortes de las columnas. */
  protected readonly cortes = CORTES;

  /** Formato de las notas, para la plantilla. */
  protected readonly formatear = formatearNota;

  /** Identificador del grupo de la URL. */
  private grupoId = 0;

  /** Inscripciones del grupo, por nombre del estudiante. */
  protected readonly inscripciones = signal<InscripcionDto[]>([]);

  /** Grupo, tomado de las inscripciones. */
  protected readonly grupo = computed(() => this.inscripciones()[0]?.grupo ?? null);

  /** Solo se registran notas en el periodo activo (RN-28); el backend igualmente lo valida. */
  protected readonly editable = computed(() => this.grupo()?.periodo.activo ?? false);

  /** Valores escritos y aún no guardados, por celda. */
  private readonly borradores = signal<Record<string, string>>({});

  /** Nota que espera confirmación. */
  protected readonly porRegistrar = signal<NotaPorRegistrar | null>(null);

  /** Indica que se está enviando la nota. */
  protected readonly registrando = signal(false);

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Mensaje del cuadro de confirmación. */
  protected readonly mensajeConfirmacion = computed(() => {
    const pendiente = this.porRegistrar();
    return pendiente
      ? `Vas a registrar ${formatearNota(pendiente.valor)} en el ${pendiente.corte.titulo.toLowerCase()} ` +
          `de ${pendiente.inscripcion.estudiante?.nombreCompleto ?? 'el estudiante'}. ` +
          'Una vez registrada, la nota no se podrá corregir.'
      : '';
  });

  /** Lee el grupo de la URL y carga sus estudiantes. */
  ngOnInit(): void {
    this.grupoId = Number(this.ruta.snapshot.paramMap.get('id'));
    this.cargar();
  }

  /**
   * Nota registrada de un corte.
   *
   * @param inscripcion inscripción de la fila
   * @param corte corte de la columna
   * @return la nota, o `null` si aún no se registra
   */
  protected nota(inscripcion: InscripcionDto, corte: DatosCorte): number | null {
    return inscripcion[corte.campo] ?? null;
  }

  /**
   * Valor escrito en una celda y aún no guardado.
   *
   * @param inscripcion inscripción de la fila
   * @param corte corte de la columna
   * @return texto escrito, o vacío
   */
  protected borrador(inscripcion: InscripcionDto, corte: DatosCorte): string {
    return this.borradores()[claveDe(inscripcion, corte)] ?? '';
  }

  /**
   * Guarda lo que el profesor escribe en una celda.
   *
   * @param inscripcion inscripción de la fila
   * @param corte corte de la columna
   * @param texto valor del campo
   */
  protected escribir(inscripcion: InscripcionDto, corte: DatosCorte, texto: string): void {
    this.borradores.update((actuales) => ({ ...actuales, [claveDe(inscripcion, corte)]: texto }));
  }

  /**
   * Revisa el valor escrito y pide confirmación antes de registrarlo.
   *
   * @param inscripcion inscripción de la fila
   * @param corte corte de la columna
   */
  protected pedirConfirmacion(inscripcion: InscripcionDto, corte: DatosCorte): void {
    const valor = leerNota(this.borrador(inscripcion, corte));
    if (valor === null) {
      this.errores.set([NOTA_INVALIDA]);
      return;
    }
    this.errores.set([]);
    this.porRegistrar.set({ inscripcion, corte, valor });
  }

  /** Envía la nota confirmada y actualiza la fila con la respuesta del backend. */
  protected registrar(): void {
    const pendiente = this.porRegistrar();
    if (!pendiente) {
      return;
    }
    this.registrando.set(true);
    this.profesor
      .registrarNota(pendiente.inscripcion.id, {
        corte: pendiente.corte.corte,
        valor: pendiente.valor,
      })
      .subscribe({
        next: (actualizada) => this.alRegistrar(pendiente, actualizada),
        error: (error: unknown) => {
          this.registrando.set(false);
          this.porRegistrar.set(null);
          this.errores.set(mensajesDe(error));
          this.cargar();
        },
      });
  }

  /**
   * Reemplaza la fila calificada, limpia su borrador y avisa; si ya están los tres cortes, el aviso
   * incluye la definitiva que calculó el backend.
   *
   * @param pendiente nota que se registró
   * @param actualizada inscripción devuelta por el backend
   */
  private alRegistrar(pendiente: NotaPorRegistrar, actualizada: InscripcionDto): void {
    this.registrando.set(false);
    this.porRegistrar.set(null);
    this.inscripciones.update((lista) =>
      lista.map((inscripcion) => (inscripcion.id === actualizada.id ? actualizada : inscripcion)),
    );
    this.escribir(pendiente.inscripcion, pendiente.corte, '');
    const definitiva =
      actualizada.notaDefinitiva === null || actualizada.notaDefinitiva === undefined
        ? ''
        : ` Definitiva: ${formatearNota(actualizada.notaDefinitiva)}.`;
    this.avisos.exito(
      `Nota ${formatearNota(pendiente.valor)} registrada en el corte ${pendiente.corte.corte}.${definitiva}`,
    );
  }

  /** Trae los estudiantes del grupo, ordenados por nombre. */
  private cargar(): void {
    this.cargando.set(true);
    this.profesor.estudiantes(this.grupoId).subscribe({
      next: (lista) => {
        this.inscripciones.set(
          [...lista].sort((primera, segunda) =>
            (primera.estudiante?.nombreCompleto ?? '').localeCompare(
              segunda.estudiante?.nombreCompleto ?? '',
            ),
          ),
        );
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
