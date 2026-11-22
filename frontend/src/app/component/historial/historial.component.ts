import { Location } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HistorialPeriodoDto } from '../../model/consulta.model';
import { InscripcionDto } from '../../model/inscripcion.model';
import { EstudianteService } from '../../service/estudiante.service';
import { ProfesorService } from '../../service/profesor.service';
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
  [...historial].sort((primero, segundo) =>
    segundo.periodo.codigo.localeCompare(primero.periodo.codigo),
  );

/**
 * Historial académico agrupado por periodo. El estudiante ve el suyo (`/estudiante/historial`); el
 * profesor, el de un estudiante de sus grupos (`/profesor/estudiantes/:id/historial`).
 */
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

  /** Consultas del profesor. */
  private readonly profesor = inject(ProfesorService);

  /** Ruta actual, para leer el estudiante consultado por el profesor. */
  private readonly ruta = inject(ActivatedRoute);

  /** Historial del navegador, para volver a la pantalla anterior. */
  private readonly ubicacion = inject(Location);

  /** Columnas de cada periodo. */
  protected readonly columnas: ColumnaTabla<InscripcionDto>[] = columnasDeNotas();

  /** Periodos con sus intentos, del más reciente al más antiguo. */
  protected readonly periodos = signal<HistorialPeriodoDto[]>([]);

  /** Indica que lo consulta un profesor (la ruta trae el id del estudiante). */
  protected readonly deProfesor = signal(false);

  /** Nombre del estudiante consultado por el profesor, tomado del primer intento. */
  protected readonly nombreEstudiante = computed(
    () => this.periodos()[0]?.inscripciones[0]?.estudiante?.nombreCompleto ?? null,
  );

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga el historial propio o el del estudiante de la URL. */
  ngOnInit(): void {
    const estudianteId = Number(this.ruta.snapshot.paramMap.get('id')) || null;
    this.deProfesor.set(estudianteId !== null);
    const consulta =
      estudianteId === null ? this.estudiante.historial() : this.profesor.historial(estudianteId);
    consulta.subscribe({
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

  /** Vuelve a la pantalla anterior (el grupo desde el que se abrió). */
  protected volver(): void {
    this.ubicacion.back();
  }
}
