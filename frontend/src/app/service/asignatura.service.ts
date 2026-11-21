import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { AsignaturaDto } from '../model/asignatura.model';

/** Llamadas a `/api/asignaturas` (plan de estudios, solo lectura). */
@Injectable({ providedIn: 'root' })
export class AsignaturaService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de las rutas del plan de estudios. */
  private readonly base = `${environment.apiUrl}/api/asignaturas`;

  /**
   * Trae las asignaturas del plan con sus prerrequisitos.
   *
   * @return todas las asignaturas
   */
  listar(): Observable<AsignaturaDto[]> {
    return this.http.get<AsignaturaDto[]>(this.base);
  }
}
