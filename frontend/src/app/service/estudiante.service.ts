import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { AsignaturaEstadoDto, ClaseDto, HistorialPeriodoDto } from '../model/consulta.model';

/** Consultas del estudiante sobre sí mismo (`/api/estudiante/...`); el estudiante sale del token. */
@Injectable({ providedIn: 'root' })
export class EstudianteService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de las consultas del estudiante. */
  private readonly base = `${environment.apiUrl}/api/estudiante`;

  /**
   * Estado de cada asignatura del plan.
   *
   * @return una fila por asignatura
   */
  planEstado(): Observable<AsignaturaEstadoDto[]> {
    return this.http.get<AsignaturaEstadoDto[]>(`${this.base}/plan-estado`);
  }

  /**
   * Horario del periodo activo.
   *
   * @return una fila por franja de las asignaturas que cursa
   */
  horario(): Observable<ClaseDto[]> {
    return this.http.get<ClaseDto[]>(`${this.base}/horario`);
  }

  /**
   * Historial académico agrupado por periodo.
   *
   * @return los periodos con sus intentos
   */
  historial(): Observable<HistorialPeriodoDto[]> {
    return this.http.get<HistorialPeriodoDto[]>(`${this.base}/historial`);
  }
}
