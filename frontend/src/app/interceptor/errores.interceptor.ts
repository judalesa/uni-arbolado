import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { leerError } from '../util/errores.util';
import { SesionService } from '../service/sesion.service';
import { ToastService } from '../service/toast.service';

/**
 * Reacciona a los errores comunes de la API en un solo lugar (mapa de pantallas, sección 2):
 * 401 cierra la sesión y lleva a `/login`; 403 por rol lleva a `/403`; sin conexión y 500 avisan con
 * un Toast. Los 400, 409, 422, 429 y los 403 de cuenta pendiente los resuelve cada pantalla, que sabe
 * qué campo o motivo mostrar. El error siempre se vuelve a lanzar para que la pantalla lo reciba.
 *
 * @param peticion petición saliente
 * @param siguiente siguiente paso de la cadena
 * @return el flujo de la respuesta
 */
export const erroresInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  const avisos = inject(ToastService);

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const codigo = leerError(error)?.codigo;
        if (error.status === 401 && codigo !== 'CREDENCIALES_INVALIDAS') {
          sesion.cerrar();
          avisos.advertencia('Tu sesión terminó. Inicia sesión de nuevo.');
          router.navigateByUrl('/login');
        } else if (error.status === 403 && codigo === 'ACCESO_DENEGADO') {
          router.navigateByUrl('/403');
        } else if (error.status === 0) {
          avisos.error('No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.');
        } else if (error.status >= 500) {
          avisos.error('Ocurrió un error en el servidor. Inténtalo más tarde.');
        }
      }
      return throwError(() => error);
    }),
  );
};
