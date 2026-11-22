import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { GrupoDto } from '../../model/grupo.model';
import { PeriodoDto } from '../../model/periodo.model';
import { ProfesorService } from '../../service/profesor.service';
import { mensajesDe } from '../../util/errores.util';
import { formatearHorarios } from '../../util/horario.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { periodoInicial } from '../grupos/grupos.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/**
 * Periodos distintos en los que el profesor tiene grupos, del más reciente al más antiguo. Se sacan de
 * los mismos grupos para no pedir la lista de periodos aparte.
 *
 * @param grupos grupos del profesor en todos los periodos
 * @return periodos sin repetir
 */
export const periodosDeGrupos = (grupos: GrupoDto[]): PeriodoDto[] =>
  [...new Map(grupos.map((grupo) => [grupo.periodo.id, grupo.periodo])).values()].sort(
    (primero, segundo) => segundo.codigo.localeCompare(primero.codigo),
  );

/** Grupos asignados al profesor, filtrados por periodo (`/profesor/grupos`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-mis-grupos',
  /** Dependencias de la plantilla. */
  imports: [FormsModule, RouterLink, TablaComponent, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './mis-grupos.component.html',
})
export class MisGruposComponent implements OnInit {
  /** Consultas del profesor. */
  private readonly profesor = inject(ProfesorService);

  /** Columnas de la tabla. */
  protected readonly columnas: ColumnaTabla<GrupoDto>[] = [
    {
      titulo: 'Asignatura',
      valor: (grupo) => `${grupo.asignatura.codigo} · ${grupo.asignatura.nombre}`,
    },
    { clave: 'numero', titulo: 'Grupo', clase: 'text-center' },
    { titulo: 'Salón', valor: (grupo) => `${grupo.bloque}-${grupo.salon}` },
    {
      titulo: 'Inscritos',
      valor: (grupo) => `${grupo.inscritos}/${grupo.cupoMaximo}`,
      clase: 'text-center',
    },
    { titulo: 'Horario', valor: (grupo) => formatearHorarios(grupo.horarios), clase: 'small' },
  ];

  /** Todos los grupos del profesor. */
  private readonly todos = signal<GrupoDto[]>([]);

  /** Periodos para el filtro. */
  protected readonly periodos = computed(() => periodosDeGrupos(this.todos()));

  /** Periodo elegido en el filtro. */
  protected readonly periodoId = signal<number | null>(null);

  /** Grupos del periodo elegido, por código de asignatura y número. */
  protected readonly visibles = computed(() =>
    this.todos()
      .filter((grupo) => grupo.periodo.id === this.periodoId())
      .sort(
        (primero, segundo) =>
          primero.asignatura.codigo.localeCompare(segundo.asignatura.codigo) ||
          primero.numero - segundo.numero,
      ),
  );

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga los grupos de todos los periodos y elige el activo. */
  ngOnInit(): void {
    this.profesor.grupos().subscribe({
      next: (grupos) => {
        this.todos.set(grupos);
        this.periodoId.set(periodoInicial(periodosDeGrupos(grupos), null));
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
