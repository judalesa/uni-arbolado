import { Routes } from '@angular/router';

/** Rutas del estudiante (`/estudiante/...`); cada pantalla se carga de forma diferida. */
export const rutasEstudiante: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'plan' },
  {
    path: 'plan',
    title: 'Mi plan de estudios',
    loadComponent: () =>
      import('../component/plan-estado/plan-estado.component').then((m) => m.PlanEstadoComponent),
  },
  {
    path: 'oferta',
    title: 'Oferta e inscripción',
    loadComponent: () =>
      import('../component/oferta/oferta.component').then((m) => m.OfertaComponent),
  },
  {
    path: 'inscripciones',
    title: 'Mis inscripciones',
    loadComponent: () =>
      import('../component/mis-inscripciones/mis-inscripciones.component').then(
        (m) => m.MisInscripcionesComponent,
      ),
  },
  {
    path: 'horario',
    title: 'Mi horario',
    loadComponent: () =>
      import('../component/horario/horario.component').then((m) => m.HorarioComponent),
  },
  {
    path: 'historial',
    title: 'Historial académico',
    loadComponent: () =>
      import('../component/historial/historial.component').then((m) => m.HistorialComponent),
  },
];
