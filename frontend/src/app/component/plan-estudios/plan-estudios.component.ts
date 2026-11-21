import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { AsignaturaDto } from '../../model/asignatura.model';
import { AsignaturaService } from '../../service/asignatura.service';
import { mensajesDe } from '../../util/errores.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/** Asignaturas de un mismo semestre con su total de créditos. */
export interface Semestre {
  /** Número del semestre. */
  numero: number;
  /** Asignaturas del semestre, por código. */
  asignaturas: AsignaturaDto[];
  /** Suma de créditos del semestre. */
  creditos: number;
}

/**
 * Agrupa las asignaturas por semestre, en orden, y ordena cada grupo por código.
 *
 * @param asignaturas plan completo
 * @return semestres con sus asignaturas
 */
export const agruparPorSemestre = (asignaturas: AsignaturaDto[]): Semestre[] => {
  const grupos = new Map<number, AsignaturaDto[]>();
  for (const asignatura of asignaturas) {
    grupos.set(asignatura.semestre, [...(grupos.get(asignatura.semestre) ?? []), asignatura]);
  }
  return [...grupos.entries()]
    .sort(([a], [b]) => a - b)
    .map(([numero, lista]) => ({
      numero,
      asignaturas: [...lista].sort((a, b) => a.codigo.localeCompare(b.codigo)),
      creditos: lista.reduce((suma, a) => suma + a.creditos, 0),
    }));
};

/** Plan de estudios del administrador (`/admin/plan`), agrupado por semestre y de solo lectura. */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-plan-estudios',
  /** Dependencias de la plantilla. */
  imports: [TablaComponent, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './plan-estudios.component.html',
})
export class PlanEstudiosComponent implements OnInit {
  /** Servicio del plan de estudios. */
  private readonly servicio = inject(AsignaturaService);

  /** Columnas de cada tabla de semestre. */
  protected readonly columnas: ColumnaTabla<AsignaturaDto>[] = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Asignatura' },
    { clave: 'creditos', titulo: 'Créditos', clase: 'text-center' },
    {
      titulo: 'Prerrequisitos',
      valor: (a) => a.prerrequisitos.map((p) => p.codigo).join(', '),
    },
  ];

  /** Asignaturas del plan. */
  private readonly asignaturas = signal<AsignaturaDto[]>([]);

  /** Semestres con sus asignaturas. */
  protected readonly semestres = computed(() => agruparPorSemestre(this.asignaturas()));

  /** Totales del plan para el encabezado. */
  protected readonly resumen = computed(() => {
    const lista = this.asignaturas();
    return {
      asignaturas: lista.length,
      creditos: lista.reduce((suma, a) => suma + a.creditos, 0),
    };
  });

  /** Indica que se está cargando el plan. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga el plan al abrir la pantalla. */
  ngOnInit(): void {
    this.servicio.listar().subscribe({
      next: (lista) => {
        this.asignaturas.set(lista);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
