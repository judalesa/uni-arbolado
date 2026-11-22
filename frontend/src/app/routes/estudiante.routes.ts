import { Routes } from '@angular/router';

/** Rutas del estudiante (`/estudiante/...`); cada pantalla se carga de forma diferida. */
export const rutasEstudiante: Routes = [
  {
    path: 'oferta',
    title: 'Oferta e inscripción',
    loadComponent: () =>
      import('../component/oferta/oferta.component').then((m) => m.OfertaComponent),
  },
];
