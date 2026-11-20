/** Roles del sistema; coinciden con el enum del contrato. */
export type Rol = 'ESTUDIANTE' | 'PROFESOR' | 'ADMINISTRADOR';

/** Estado de verificación de una cuenta. */
export type EstadoCuenta = 'PENDIENTE' | 'ACTIVO';

/** Usuario tal como lo devuelve la API (`UsuarioDto`). */
export interface UsuarioDto {
  /** Identificador. */
  id: number;
  /** Documento de 10 dígitos. */
  documento: string;
  /** Nombre completo. */
  nombreCompleto: string;
  /** Correo electrónico. */
  correo: string;
  /** Rol del usuario. */
  rol: Rol;
  /** Estado de la cuenta. */
  estado: EstadoCuenta;
  /** Instante de creación (UTC). */
  fechaCreacion?: string;
}

/** Datos mínimos de un usuario dentro de otro objeto (`UsuarioResumen`). */
export interface UsuarioResumen {
  /** Identificador. */
  id: number;
  /** Documento. */
  documento: string;
  /** Nombre completo. */
  nombreCompleto: string;
}
