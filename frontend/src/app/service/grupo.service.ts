import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { GrupoDto, GrupoRequest } from '../model/grupo.model';

/** Llamadas a `/api/admin/grupos`. */
@Injectable({ providedIn: 'root' })
export class GrupoService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de las rutas de grupos del administrador. */
  private readonly base = `${environment.apiUrl}/api/admin/grupos`;

  /**
   * Lista los grupos, opcionalmente de un solo periodo.
   *
   * @param periodoId periodo a filtrar; `null` para todos
   * @return los grupos
   */
  listar(periodoId: number | null = null): Observable<GrupoDto[]> {
    const params = periodoId === null ? new HttpParams() : new HttpParams().set('periodoId', periodoId);
    return this.http.get<GrupoDto[]>(this.base, { params });
  }

  /**
   * Consulta un grupo.
   *
   * @param id identificador del grupo
   * @return el grupo
   */
  obtener(id: number): Observable<GrupoDto> {
    return this.http.get<GrupoDto>(`${this.base}/${id}`);
  }

  /**
   * Crea un grupo; el número y el cupo los asigna el backend.
   *
   * @param datos datos del grupo
   * @return el grupo creado
   */
  crear(datos: GrupoRequest): Observable<GrupoDto> {
    return this.http.post<GrupoDto>(this.base, datos);
  }

  /**
   * Modifica un grupo. Con inscritos, el backend solo acepta cambios de profesor, bloque y salón.
   *
   * @param id identificador del grupo
   * @param datos datos del grupo
   * @return el grupo modificado
   */
  modificar(id: number, datos: GrupoRequest): Observable<GrupoDto> {
    return this.http.put<GrupoDto>(`${this.base}/${id}`, datos);
  }
}
