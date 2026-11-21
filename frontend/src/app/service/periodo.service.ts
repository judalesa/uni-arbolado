import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { PeriodoDto, PeriodoRequest } from '../model/periodo.model';

/** Llamadas a `/api/periodos`. */
@Injectable({ providedIn: 'root' })
export class PeriodoService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de las rutas de periodos. */
  private readonly base = `${environment.apiUrl}/api/periodos`;

  /**
   * Lista todos los periodos.
   *
   * @return los periodos
   */
  listar(): Observable<PeriodoDto[]> {
    return this.http.get<PeriodoDto[]>(this.base);
  }

  /**
   * Crea un periodo; nace inactivo.
   *
   * @param datos código y fechas
   * @return el periodo creado
   */
  crear(datos: PeriodoRequest): Observable<PeriodoDto> {
    return this.http.post<PeriodoDto>(this.base, datos);
  }

  /**
   * Modifica el código y las fechas de un periodo; no cambia si está activo.
   *
   * @param id identificador del periodo
   * @param datos código y fechas
   * @return el periodo modificado
   */
  modificar(id: number, datos: PeriodoRequest): Observable<PeriodoDto> {
    return this.http.put<PeriodoDto>(`${this.base}/${id}`, datos);
  }

  /**
   * Activa un periodo; el backend desactiva el anterior en la misma operación.
   *
   * @param id identificador del periodo
   * @return el periodo activado
   */
  activar(id: number): Observable<PeriodoDto> {
    return this.http.post<PeriodoDto>(`${this.base}/${id}/activar`, null);
  }
}
