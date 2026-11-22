import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { AsignaturaDto } from '../../model/asignatura.model';
import { AsignaturaEstadoDto } from '../../model/consulta.model';
import { EstudianteService } from '../../service/estudiante.service';
import { mensajesDe } from '../../util/errores.util';
import { formatearNota } from '../../util/notas.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { EtiquetaEstadoComponent } from '../etiqueta-estado/etiqueta-estado.component';
import { agruparPorSemestre } from '../plan-estudios/plan-estudios.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/** Totales del avance del estudiante. */
export interface Avance {
  /** Asignaturas aprobadas. */
  aprobadas: number;
  /** Asignaturas del plan. */
  total: number;
  /** Créditos aprobados. */
  creditosAprobados: number;
  /** Créditos del plan. */
  creditosTotales: number;
}

/**
 * Calcula el avance del estudiante a partir del estado de cada asignatura.
 *
 * @param plan plan con estados
 * @return totales del avance
 */
export const calcularAvance = (plan: AsignaturaEstadoDto[]): Avance => {
  const aprobadas = plan.filter((fila) => fila.estado === 'APROBADA');
  return {
    aprobadas: aprobadas.length,
    total: plan.length,
    creditosAprobados: aprobadas.reduce((suma, fila) => suma + fila.asignatura.creditos, 0),
    creditosTotales: plan.reduce((suma, fila) => suma + fila.asignatura.creditos, 0),
  };
};

/** Plan de estudios con el estado de cada asignatura para el estudiante (`/estudiante/plan`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-plan-estado',
  /** Dependencias de la plantilla. */
  imports: [TablaComponent, AlertaErroresComponent, EtiquetaEstadoComponent],
  /** Plantilla HTML. */
  templateUrl: './plan-estado.component.html',
})
export class PlanEstadoComponent implements OnInit {
  /** Consultas del estudiante. */
  private readonly servicio = inject(EstudianteService);

  /** Plan con estados. */
  private readonly plan = signal<AsignaturaEstadoDto[]>([]);

  /** Estado y nota de cada asignatura, por id. */
  private readonly porAsignatura = computed(
    () => new Map(this.plan().map((fila) => [fila.asignatura.id, fila])),
  );

  /** Columnas de cada tabla de semestre; el estado va en la columna final como etiqueta de color. */
  protected readonly columnas: ColumnaTabla<AsignaturaDto>[] = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Asignatura' },
    { clave: 'creditos', titulo: 'Créditos', clase: 'text-center' },
    { titulo: 'Prerrequisitos', valor: (a) => a.prerrequisitos.map((p) => p.codigo).join(', ') },
    {
      titulo: 'Definitiva',
      valor: (a) => {
        const nota = this.porAsignatura().get(a.id)?.notaDefinitiva;
        return nota === null || nota === undefined ? '' : formatearNota(nota);
      },
      clase: 'text-center',
    },
  ];

  /** Semestres del plan. */
  protected readonly semestres = computed(() =>
    agruparPorSemestre(this.plan().map((fila) => fila.asignatura)),
  );

  /** Totales del avance. */
  protected readonly avance = computed(() => calcularAvance(this.plan()));

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga el plan con estados. */
  ngOnInit(): void {
    this.servicio.planEstado().subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }

  /**
   * Estado de una asignatura para la etiqueta de color.
   *
   * @param asignatura asignatura de la fila
   * @return código del estado
   */
  protected estadoDe(asignatura: AsignaturaDto): string {
    return this.porAsignatura().get(asignatura.id)?.estado ?? 'POR_CURSAR';
  }
}
