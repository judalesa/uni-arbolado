import { Routes } from '@angular/router';

/** Rutas de la aplicación; cada pantalla se carga de forma diferida. */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Inicio',
    loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
  },
  { path: '**', redirectTo: '' },
];
