import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';

/** Punto de entrada: monta el componente raíz con la configuración global. */
bootstrapApplication(App, appConfig).catch((err) => console.error(err));
