import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { AuditoriaDto, FiltroAuditoria } from '../model/auditoria.model';
import { Pagina } from '../model/pagina.model';

/** Consulta de la auditoría del administrador (`/api/admin/auditoria`); no hay edición ni borrado. */
@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Ruta de la auditoría. */
  private readonly url = `${environment.apiUrl}/api/admin/auditoria`;

  /**
   * Consulta una página de registros, del más reciente al más antiguo.
   *
   * @param filtro filtros a aplicar; los vacíos se omiten
   * @param pagina número de página desde 0
   * @param tamano registros por página
   * @return la página pedida
   */
  consultar(filtro: FiltroAuditoria, pagina = 0, tamano = 20): Observable<Pagina<AuditoriaDto>> {
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    for (const [nombre, valor] of Object.entries(filtro)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(nombre, valor);
      }
    }
    return this.http.get<Pagina<AuditoriaDto>>(this.url, { params });
  }
}
