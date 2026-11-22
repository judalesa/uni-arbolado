import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { ClaseDto, HistorialPeriodoDto } from '../model/consulta.model';
import { GrupoDto } from '../model/grupo.model';
import { InscripcionDto, NotaRequest } from '../model/inscripcion.model';

/**
 * Consultas y registro de notas del profesor (`/api/profesor/...`). El profesor sale del token y el
 * backend rechaza con 403 los grupos o estudiantes que no son suyos.
 */
@Injectable({ providedIn: 'root' })
export class ProfesorService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de las rutas del profesor. */
  private readonly base = `${environment.apiUrl}/api/profesor`;

  /**
   * Grupos asignados al profesor.
   *
   * @param periodoId periodo a consultar, o `null` para todos los periodos
   * @return grupos del profesor
   */
  grupos(periodoId: number | null = null): Observable<GrupoDto[]> {
    const params =
      periodoId === null ? new HttpParams() : new HttpParams().set('periodoId', periodoId);
    return this.http.get<GrupoDto[]>(`${this.base}/grupos`, { params });
  }

  /**
   * Estudiantes de uno de sus grupos, con sus notas.
   *
   * @param grupoId grupo a consultar
   * @return inscripciones del grupo
   */
  estudiantes(grupoId: number): Observable<InscripcionDto[]> {
    return this.http.get<InscripcionDto[]>(`${this.base}/grupos/${grupoId}/estudiantes`);
  }

  /**
   * Registra la nota de un corte; cada corte se registra una sola vez.
   *
   * @param inscripcionId inscripción a calificar
   * @param nota corte y valor
   * @return inscripción actualizada, con la definitiva si ya están los tres cortes
   */
  registrarNota(inscripcionId: number, nota: NotaRequest): Observable<InscripcionDto> {
    return this.http.post<InscripcionDto>(
      `${this.base}/inscripciones/${inscripcionId}/notas`,
      nota,
    );
  }

  /**
   * Horario del profesor en el periodo activo.
   *
   * @return una fila por franja de sus grupos
   */
  horario(): Observable<ClaseDto[]> {
    return this.http.get<ClaseDto[]>(`${this.base}/horario`);
  }

  /**
   * Historial de un estudiante inscrito en alguno de sus grupos.
   *
   * @param estudianteId estudiante a consultar
   * @return los periodos con sus intentos
   */
  historial(estudianteId: number): Observable<HistorialPeriodoDto[]> {
    return this.http.get<HistorialPeriodoDto[]>(
      `${this.base}/estudiantes/${estudianteId}/historial`,
    );
  }
}
