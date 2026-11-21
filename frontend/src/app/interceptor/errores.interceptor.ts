import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { leerError } from '../util/errores.util';
import { SesionService } from '../service/sesion.service';
import { ToastService } from '../service/toast.service';

/** Reacción global a un tipo de error HTTP. */
interface Reaccion {
  /** Indica si la reacción corresponde al estado y al código del error. */
  aplica: (status: number, codigo: string | undefined) => boolean;
  /** Acción que se ejecuta. */
  ejecutar: () => void;
}

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

  /** Reacciones globales, en orden; se aplica la primera que corresponda. */
  const reacciones: Reaccion[] = [
    {
      aplica: (status, codigo) => status === 401 && codigo !== 'CREDENCIALES_INVALIDAS',
      ejecutar: () => {
        sesion.cerrar();
        avisos.advertencia('Tu sesión terminó. Inicia sesión de nuevo.');
        router.navigateByUrl('/login');
      },
    },
    {
      aplica: (status, codigo) => status === 403 && codigo === 'ACCESO_DENEGADO',
      ejecutar: () => router.navigateByUrl('/403'),
    },
    {
      aplica: (status) => status === 0,
      ejecutar: () =>
        avisos.error('No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.'),
    },
    {
      aplica: (status) => status >= 500,
      ejecutar: () => avisos.error('Ocurrió un error en el servidor. Inténtalo más tarde.'),
    },
  ];

  return siguiente(peticion).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const codigo = leerError(error)?.codigo;
        reacciones.find((reaccion) => reaccion.aplica(error.status, codigo))?.ejecutar();
      }
      return throwError(() => error);
    }),
  );
};
