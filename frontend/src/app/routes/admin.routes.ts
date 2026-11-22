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
    path: 'grupos',
    title: 'Grupos',
    loadComponent: () =>
      import('../component/grupos/grupos.component').then((m) => m.GruposComponent),
  },
  {
    path: 'grupos/nuevo',
    title: 'Nuevo grupo',
    loadComponent: () =>
      import('../component/grupo-formulario/grupo-formulario.component').then(
        (m) => m.GrupoFormularioComponent,
      ),
  },
  {
    path: 'grupos/:id',
    title: 'Modificar grupo',
    loadComponent: () =>
      import('../component/grupo-formulario/grupo-formulario.component').then(
        (m) => m.GrupoFormularioComponent,
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
  {
    path: 'auditoria',
    title: 'Auditoría',
    loadComponent: () =>
      import('../component/auditoria/auditoria.component').then((m) => m.AuditoriaComponent),
  },
];
