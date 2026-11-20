import { Component, computed, input } from '@angular/core';

/** Texto y color de cada estado conocido. */
const ESTADOS: Record<string, { texto: string; clase: string }> = {
  POR_CURSAR: { texto: 'Por cursar', clase: 'text-bg-secondary' },
  CURSANDO: { texto: 'Cursando', clase: 'text-bg-primary' },
  APROBADA: { texto: 'Aprobada', clase: 'text-bg-success' },
  REPROBADA: { texto: 'Reprobada', clase: 'text-bg-danger' },
  ACTIVO: { texto: 'Activo', clase: 'text-bg-success' },
  PENDIENTE: { texto: 'Pendiente', clase: 'text-bg-warning' },
};

/** Etiqueta de color para un estado de asignatura o de cuenta (mapa de pantallas, sección 2). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-etiqueta-estado',
  /** Plantilla en línea: es un solo elemento. */
  template: `<span class="badge" [class]="datos().clase">{{ datos().texto }}</span>`,
})
export class EtiquetaEstadoComponent {
  /** Código del estado, tal como llega de la API. */
  readonly estado = input.required<string>();

  /** Texto y clase del estado; un estado desconocido se muestra en gris con su propio código. */
  protected readonly datos = computed(
    () => ESTADOS[this.estado()] ?? { texto: this.estado(), clase: 'text-bg-secondary' },
  );
}
