import { Routes } from '@angular/router';

import { rolGuard } from './guard/acceso.guard';

/** Rutas de la aplicación. Cada rama por rol se carga de forma diferida y solo si el rol coincide. */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Inicio',
    loadComponent: () =>
      import('./component/inicio/inicio.component').then((m) => m.InicioComponent),
  },
  {
    path: 'estudiante',
    canMatch: [rolGuard('ESTUDIANTE')],
    loadChildren: () => import('./routes/estudiante.routes').then((m) => m.rutasEstudiante),
  },
  {
    path: 'profesor',
    canMatch: [rolGuard('PROFESOR')],
    loadChildren: () => import('./routes/profesor.routes').then((m) => m.rutasProfesor),
  },
  {
    path: 'admin',
    canMatch: [rolGuard('ADMINISTRADOR')],
    loadChildren: () => import('./routes/admin.routes').then((m) => m.rutasAdmin),
  },
  {
    path: '403',
    title: 'Acceso denegado',
    data: {
      codigo: '403',
      titulo: 'Acceso denegado',
      mensaje: 'No tienes permiso para ver esta pantalla.',
    },
    loadComponent: () =>
      import('./component/pagina-error/pagina-error.component').then((m) => m.PaginaErrorComponent),
  },
  {
    path: '**',
    title: 'No encontrada',
    data: {
      codigo: '404',
      titulo: 'Página no encontrada',
      mensaje: 'La dirección no existe o fue movida.',
    },
    loadComponent: () =>
      import('./component/pagina-error/pagina-error.component').then((m) => m.PaginaErrorComponent),
  },
];
