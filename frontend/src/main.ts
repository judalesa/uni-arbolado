import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';

/**
 * Punto de entrada del frontend: el navegador ejecuta este archivo primero.
 * Monta el componente raíz `App` dentro de la etiqueta `<app-root>` de `index.html`, usando la
 * configuración global definida en `app.config.ts`.
 */
bootstrapApplication(App, appConfig)
  // Si el arranque falla (por ejemplo, un error de configuración), se muestra en la consola.
  .catch((err) => console.error(err));
