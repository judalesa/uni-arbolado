import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { InscripcionDto } from '../../model/inscripcion.model';
import { InscripcionService } from '../../service/inscripcion.service';
import { mensajesDe } from '../../util/errores.util';
import { columnasDeNotas } from '../../util/notas.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { EtiquetaEstadoComponent } from '../etiqueta-estado/etiqueta-estado.component';
import { ColumnaTabla, TablaComponent } from '../tabla/tabla.component';

/** Inscripciones del periodo activo con las notas por corte (`/estudiante/inscripciones`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-mis-inscripciones',
  /** Dependencias de la plantilla. */
  imports: [TablaComponent, AlertaErroresComponent, EtiquetaEstadoComponent],
  /** Plantilla HTML. */
  templateUrl: './mis-inscripciones.component.html',
})
export class MisInscripcionesComponent implements OnInit {
  /** Servicio de inscripciones. */
  private readonly servicio = inject(InscripcionService);

  /** Columnas: las de notas más el profesor. */
  protected readonly columnas: ColumnaTabla<InscripcionDto>[] = [
    ...columnasDeNotas().slice(0, 3),
    { titulo: 'Profesor', valor: (i) => i.grupo.profesor.nombreCompleto },
    ...columnasDeNotas().slice(3),
  ];

  /** Inscripciones del periodo activo. */
  protected readonly inscripciones = signal<InscripcionDto[]>([]);

  /** Código del periodo, tomado de las inscripciones. */
  protected readonly periodo = computed(() => this.inscripciones()[0]?.grupo.periodo.codigo ?? null);

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga las inscripciones del periodo activo. */
  ngOnInit(): void {
    this.servicio.mias().subscribe({
      next: (lista) => {
        this.inscripciones.set(
          [...lista].sort((primera, segunda) =>
            primera.grupo.asignatura.codigo.localeCompare(segunda.grupo.asignatura.codigo),
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
