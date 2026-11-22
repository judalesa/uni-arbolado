import { Routes } from '@angular/router';

/** Rutas del profesor (`/profesor/...`); cada pantalla se carga de forma diferida. */
export const rutasProfesor: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'grupos' },
  {
    path: 'grupos',
    title: 'Mis grupos',
    loadComponent: () =>
      import('../component/mis-grupos/mis-grupos.component').then((m) => m.MisGruposComponent),
  },
  {
    path: 'grupos/:id',
    title: 'Registro de notas',
    loadComponent: () =>
      import('../component/registro-notas/registro-notas.component').then(
        (m) => m.RegistroNotasComponent,
      ),
  },
  {
    path: 'horario',
    title: 'Mi horario',
    data: { rol: 'PROFESOR' },
    loadComponent: () =>
      import('../component/horario/horario.component').then((m) => m.HorarioComponent),
  },
  {
    path: 'estudiantes/:id/historial',
    title: 'Historial del estudiante',
    loadComponent: () =>
      import('../component/historial/historial.component').then((m) => m.HistorialComponent),
  },
];
