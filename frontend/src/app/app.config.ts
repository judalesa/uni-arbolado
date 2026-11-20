import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { erroresInterceptor } from './interceptor/errores.interceptor';
import { tokenInterceptor } from './interceptor/token.interceptor';

/** Configuración global: rutas y cliente HTTP con el interceptor del token y el de errores. */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([tokenInterceptor, erroresInterceptor])),
  ],
};
