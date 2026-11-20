import { Rol } from '../model/usuario.model';

/** Opción del menú de navegación. */
export interface ItemMenu {
  /** Texto visible. */
  etiqueta: string;
  /** Ruta de destino. */
  ruta: string;
  /** Clase de Font Awesome del ícono. */
  icono: string;
}

/**
 * Pantalla a la que se lleva a cada rol después de iniciar sesión (mapa de pantallas, sección 1).
 *
 * @param rol rol del usuario
 * @return ruta inicial
 */
export function rutaInicial(rol: Rol): string {
  switch (rol) {
    case 'ESTUDIANTE':
      return '/estudiante/plan';
    case 'PROFESOR':
      return '/profesor/grupos';
    case 'ADMINISTRADOR':
      return '/admin/periodos';
  }
}

/** Menú de cada rol: solo se muestra lo que el rol puede usar (el backend igualmente valida). */
export const MENU_POR_ROL: Record<Rol, ItemMenu[]> = {
  ESTUDIANTE: [
    { etiqueta: 'Plan de estudios', ruta: '/estudiante/plan', icono: 'fa-list-check' },
    { etiqueta: 'Oferta e inscripción', ruta: '/estudiante/oferta', icono: 'fa-pen-to-square' },
    {
      etiqueta: 'Mis inscripciones',
      ruta: '/estudiante/inscripciones',
      icono: 'fa-clipboard-list',
    },
    { etiqueta: 'Mi horario', ruta: '/estudiante/horario', icono: 'fa-calendar-week' },
    { etiqueta: 'Historial', ruta: '/estudiante/historial', icono: 'fa-clock-rotate-left' },
    { etiqueta: 'Mi perfil', ruta: '/perfil', icono: 'fa-user' },
  ],
  PROFESOR: [
    { etiqueta: 'Mis grupos', ruta: '/profesor/grupos', icono: 'fa-users' },
    { etiqueta: 'Mi horario', ruta: '/profesor/horario', icono: 'fa-calendar-week' },
    { etiqueta: 'Mi perfil', ruta: '/perfil', icono: 'fa-user' },
  ],
  ADMINISTRADOR: [
    { etiqueta: 'Periodos', ruta: '/admin/periodos', icono: 'fa-calendar-days' },
    { etiqueta: 'Plan de estudios', ruta: '/admin/plan', icono: 'fa-list-check' },
    { etiqueta: 'Grupos', ruta: '/admin/grupos', icono: 'fa-people-group' },
    { etiqueta: 'Profesores', ruta: '/admin/profesores', icono: 'fa-chalkboard-user' },
    { etiqueta: 'Estudiantes', ruta: '/admin/estudiantes', icono: 'fa-user-graduate' },
    { etiqueta: 'Auditoría', ruta: '/admin/auditoria', icono: 'fa-shield-halved' },
    { etiqueta: 'Mi perfil', ruta: '/perfil', icono: 'fa-user' },
  ],
};
