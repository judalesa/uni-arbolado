import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { GrupoDto } from '../../model/grupo.model';
import { PeriodoDto } from '../../model/periodo.model';
import { GrupoService } from '../../service/grupo.service';
import { PeriodoService } from '../../service/periodo.service';
import { mensajesDe } from '../../util/errores.util';
import { formatearHorarios } from '../../util/horario.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/**
 * Periodo que se muestra al abrir la pantalla: el de la URL si existe; si no, el activo; si no hay
 * activo, el más reciente.
 *
 * @param periodos periodos ordenados del más reciente al más antiguo
 * @param pedido identificador que llega en la URL, o `null`
 * @return identificador del periodo elegido, o `null` si no hay periodos
 */
export const periodoInicial = (periodos: PeriodoDto[], pedido: number | null): number | null =>
  (periodos.find((p) => p.id === pedido) ?? periodos.find((p) => p.activo) ?? periodos[0])?.id ?? null;

/** Pantalla de grupos del administrador (`/admin/grupos`): listado por periodo. */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-grupos',
  /** Dependencias de la plantilla. */
  imports: [FormsModule, RouterLink, TablaComponent, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './grupos.component.html',
})
export class GruposComponent implements OnInit {
  /** Servicio de grupos. */
  private readonly grupos = inject(GrupoService);

  /** Servicio de periodos. */
  private readonly periodosServicio = inject(PeriodoService);

  /** Ruta actual, para leer el periodo de la URL. */
  private readonly ruta = inject(ActivatedRoute);

  /** Enrutador, para guardar el periodo elegido en la URL. */
  private readonly router = inject(Router);

  /** Columnas de la tabla. */
  protected readonly columnas: ColumnaTabla<GrupoDto>[] = [
    { titulo: 'Asignatura', valor: (g) => `${g.asignatura.codigo} · ${g.asignatura.nombre}` },
    { clave: 'numero', titulo: 'Grupo', clase: 'text-center' },
    { titulo: 'Profesor', valor: (g) => g.profesor.nombreCompleto },
    { titulo: 'Salón', valor: (g) => `${g.bloque}-${g.salon}` },
    { titulo: 'Inscritos', valor: (g) => `${g.inscritos}/${g.cupoMaximo}`, clase: 'text-center' },
    { titulo: 'Horario', valor: (g) => formatearHorarios(g.horarios), clase: 'small' },
  ];

  /** Periodos para el filtro, del más reciente al más antiguo. */
  protected readonly periodos = signal<PeriodoDto[]>([]);

  /** Periodo elegido en el filtro. */
  protected readonly periodoId = signal<number | null>(null);

  /** Grupos del periodo elegido. */
  protected readonly lista = signal<GrupoDto[]>([]);

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga los periodos y, con ellos, los grupos del periodo inicial. */
  ngOnInit(): void {
    const pedido = Number(this.ruta.snapshot.queryParamMap.get('periodoId')) || null;
    this.periodosServicio.listar().subscribe({
      next: (lista) => {
        const ordenados = [...lista].sort((a, b) => b.codigo.localeCompare(a.codigo));
        this.periodos.set(ordenados);
        this.periodoId.set(periodoInicial(ordenados, pedido));
        this.cargar();
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }

  /**
   * Cambia el periodo del filtro, lo deja en la URL y recarga.
   *
   * @param id periodo elegido
   */
  protected cambiarPeriodo(id: number | null): void {
    this.periodoId.set(id);
    this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { periodoId: id },
      replaceUrl: true,
    });
    this.cargar();
  }

  /** Trae los grupos del periodo elegido, ordenados por asignatura y número. */
  private cargar(): void {
    const periodo = this.periodoId();
    if (periodo === null) {
      this.lista.set([]);
      this.cargando.set(false);
      return;
    }
    this.cargando.set(true);
    this.errores.set([]);
    this.grupos.listar(periodo).subscribe({
      next: (lista) => {
        this.lista.set(
          [...lista].sort(
            (a, b) => a.asignatura.codigo.localeCompare(b.asignatura.codigo) || a.numero - b.numero,
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
