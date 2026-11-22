import { Component, OnInit, inject, signal } from '@angular/core';

import { HistorialPeriodoDto } from '../../model/consulta.model';
import { InscripcionDto } from '../../model/inscripcion.model';
import { EstudianteService } from '../../service/estudiante.service';
import { mensajesDe } from '../../util/errores.util';
import { columnasDeNotas } from '../../util/notas.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { EtiquetaEstadoComponent } from '../etiqueta-estado/etiqueta-estado.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/**
 * Ordena el historial del periodo más reciente al más antiguo.
 *
 * @param historial periodos con sus intentos
 * @return nueva lista ordenada
 */
export const ordenarHistorial = (historial: HistorialPeriodoDto[]): HistorialPeriodoDto[] =>
  [...historial].sort((a, b) => b.periodo.codigo.localeCompare(a.periodo.codigo));

/** Historial académico del estudiante agrupado por periodo (`/estudiante/historial`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-historial',
  /** Dependencias de la plantilla. */
  imports: [TablaComponent, AlertaErroresComponent, EtiquetaEstadoComponent],
  /** Plantilla HTML. */
  templateUrl: './historial.component.html',
})
export class HistorialComponent implements OnInit {
  /** Consultas del estudiante. */
  private readonly estudiante = inject(EstudianteService);

  /** Columnas de cada periodo. */
  protected readonly columnas: ColumnaTabla<InscripcionDto>[] = columnasDeNotas();

  /** Periodos con sus intentos, del más reciente al más antiguo. */
  protected readonly periodos = signal<HistorialPeriodoDto[]>([]);

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga el historial. */
  ngOnInit(): void {
    this.estudiante.historial().subscribe({
      next: (historial) => {
        this.periodos.set(ordenarHistorial(historial));
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
