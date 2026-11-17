import { Routes } from '@angular/router';

/**
 * Tabla de rutas de la aplicación.
 *
 * Cada pantalla se carga de forma diferida (`loadComponent`): el navegador descarga su código solo cuando
 * la persona entra a esa ruta, lo que reduce el tamaño inicial. Las rutas por rol (estudiante, profesor,
 * administrador) se agregarán con `loadChildren` y su guard en las siguientes funcionalidades.
 */
export const routes: Routes = [
  {
    // Pantalla de bienvenida.
    path: '',
    pathMatch: 'full',
    title: 'Inicio',
    loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
  },
  {
    // Cualquier ruta desconocida regresa al inicio.
    path: '**',
    redirectTo: '',
  },
];
