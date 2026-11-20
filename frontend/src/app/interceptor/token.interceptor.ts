import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { environment } from '../../environments/environment';
import { SesionService } from '../service/sesion.service';

/**
 * Agrega `Authorization: Bearer <token>` a las peticiones dirigidas al backend. Solo a esa URL base:
 * así el token nunca viaja a un tercero.
 *
 * @param peticion petición saliente
 * @param siguiente siguiente paso de la cadena
 * @return la petición con el token cuando corresponde
 */
export const tokenInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const token = inject(SesionService).token();
  if (token !== null && peticion.url.startsWith(environment.apiUrl)) {
    return siguiente(peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return siguiente(peticion);
};
