import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ClaseDto } from '../../model/consulta.model';
import { EstudianteService } from '../../service/estudiante.service';
import { ProfesorService } from '../../service/profesor.service';
import { mensajesDe } from '../../util/errores.util';
import { DIAS, armarCuadricula } from '../../util/horario.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';

/**
 * Horario semanal del periodo activo en una cuadrícula de lunes a sábado. La misma pantalla sirve al
 * estudiante (`/estudiante/horario`) y al profesor (`/profesor/horario`); la ruta del profesor lo
 * indica con `data.rol`.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-horario',
  /** Dependencias de la plantilla. */
  imports: [AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './horario.component.html',
  /** Estilos propios de la cuadrícula. */
  styleUrl: './horario.component.css',
})
export class HorarioComponent implements OnInit {
  /** Consultas del estudiante. */
  private readonly estudiante = inject(EstudianteService);

  /** Consultas del profesor. */
  private readonly profesor = inject(ProfesorService);

  /** Ruta actual, para saber de qué rol es el horario. */
  private readonly ruta = inject(ActivatedRoute);

  /** Días de las columnas. */
  protected readonly dias = DIAS;

  /** Clases del periodo activo. */
  private readonly clases = signal<ClaseDto[]>([]);

  /** Filas de la cuadrícula, una por franja. */
  protected readonly filas = computed(() => armarCuadricula(this.clases()));

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga el horario del rol que indica la ruta. */
  ngOnInit(): void {
    const esProfesor = this.ruta.snapshot.data['rol'] === 'PROFESOR';
    const consulta = esProfesor ? this.profesor.horario() : this.estudiante.horario();
    consulta.subscribe({
      next: (clases) => {
        this.clases.set(clases);
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
