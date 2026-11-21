import { Routes } from '@angular/router';

/** Rutas del administrador (`/admin/...`); cada pantalla se carga de forma diferida. */
export const rutasAdmin: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'periodos' },
  {
    path: 'periodos',
    title: 'Periodos',
    loadComponent: () =>
      import('../component/periodos/periodos.component').then((m) => m.PeriodosComponent),
  },
  {
    path: 'plan',
    title: 'Plan de estudios',
    loadComponent: () =>
      import('../component/plan-estudios/plan-estudios.component').then(
        (m) => m.PlanEstudiosComponent,
      ),
  },
  {
    path: 'profesores',
    title: 'Profesores',
    data: { rol: 'PROFESOR' },
    loadComponent: () =>
      import('../component/usuarios/usuarios.component').then((m) => m.UsuariosComponent),
  },
  {
    path: 'estudiantes',
    title: 'Estudiantes',
    data: { rol: 'ESTUDIANTE' },
    loadComponent: () =>
      import('../component/usuarios/usuarios.component').then((m) => m.UsuariosComponent),
  },
];
