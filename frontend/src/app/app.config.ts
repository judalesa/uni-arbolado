import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

/**
 * Configuración global de la aplicación: lista de "proveedores" que Angular registra al arrancar.
 *
 * Aquí se irán agregando, en las siguientes funcionalidades, el cliente HTTP con el interceptor del token
 * y los demás servicios globales.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Envía a la consola los errores no capturados del navegador (útil para depurar).
    provideBrowserGlobalErrorListeners(),
    // Activa el enrutador con las rutas definidas en app.routes.ts.
    provideRouter(routes),
  ],
};
