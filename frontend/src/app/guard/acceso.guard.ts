import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { Rol } from '../model/usuario.model';
import { rutaInicial } from '../util/rutas-por-rol.util';
import { SesionService } from '../service/sesion.service';

/**
 * Crea un guard que deja pasar solo a los roles indicados. Es `canMatch` y no `canActivate`: si no
 * corresponde, el módulo de ese rol ni siquiera se descarga. Es comodidad; el backend valida igual.
 *
 * @param roles roles permitidos
 * @return guard de ruta: sin sesión lleva a `/login`, con otro rol a `/403`
 */
export const rolGuard =
  (...roles: Rol[]): CanMatchFn =>
  () => {
    const sesion = inject(SesionService);
    const router = inject(Router);
    const rol = sesion.rol();
    if (!sesion.estaVigente() || rol === null) {
      return router.createUrlTree(['/login']);
    }
    return roles.includes(rol) ? true : router.createUrlTree(['/403']);
  };

/**
 * Guard de rutas para cualquier usuario con sesión (por ejemplo el perfil).
 *
 * @return guard de ruta: sin sesión lleva a `/login`
 */
export const autenticadoGuard: CanMatchFn = () => {
  const sesion = inject(SesionService);
  return sesion.estaVigente() ? true : inject(Router).createUrlTree(['/login']);
};

/**
 * Guard de las pantallas públicas (login y registro): quien ya inició sesión va a su pantalla inicial.
 *
 * @return guard de ruta
 */
export const visitanteGuard: CanMatchFn = () => {
  const sesion = inject(SesionService);
  const rol = sesion.rol();
  return sesion.estaVigente() && rol !== null
    ? inject(Router).createUrlTree([rutaInicial(rol)])
    : true;
};
