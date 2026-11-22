import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { InscripcionDto } from '../model/inscripcion.model';

/** Llamadas de inscripción del estudiante: `/api/inscripciones` y `/api/estudiante/inscripciones`. */
@Injectable({ providedIn: 'root' })
export class InscripcionService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de la API. */
  private readonly api = `${environment.apiUrl}/api`;

  /**
   * Inscribe al estudiante de la sesión en un grupo. Si alguna regla falla, el backend responde 422
   * con todos los motivos.
   *
   * @param grupoId grupo elegido
   * @return la inscripción creada, en estado `CURSANDO`
   */
  inscribir(grupoId: number): Observable<InscripcionDto> {
    return this.http.post<InscripcionDto>(`${this.api}/inscripciones`, { grupoId });
  }

  /**
   * Inscripciones del estudiante con sus notas.
   *
   * @param periodoId periodo a consultar; `null` para el activo
   * @return las inscripciones del periodo
   */
  mias(periodoId: number | null = null): Observable<InscripcionDto[]> {
    const params = periodoId === null ? new HttpParams() : new HttpParams().set('periodoId', periodoId);
    return this.http.get<InscripcionDto[]>(`${this.api}/estudiante/inscripciones`, { params });
  }
}
