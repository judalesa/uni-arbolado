import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/component/app/app.component';
import { appConfig } from './app/app.config';

/** Punto de entrada: monta el componente raíz con la configuración global. */
bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
