import { UsuarioDto } from './usuario.model';

/** Cuerpo de `POST /api/auth/login`. */
export interface LoginRequest {
  /** Correo de la cuenta. */
  correo: string;
  /** Contraseña. */
  contrasena: string;
}

/** Respuesta de `POST /api/auth/login`. */
export interface LoginResponse {
  /** JWT. */
  token: string;
  /** Tipo de token (`Bearer`). */
  tipo: string;
  /** Instante de vencimiento (UTC). */
  expiraEn: string;
  /** Usuario autenticado. */
  usuario: UsuarioDto;
}

/** Cuerpo de `POST /api/auth/registro`. */
export interface RegistroRequest {
  /** Documento de 10 dígitos. */
  documento: string;
  /** Nombre completo. */
  nombreCompleto: string;
  /** Correo electrónico. */
  correo: string;
  /** Contraseña de 8 a 72 caracteres. */
  contrasena: string;
}

/** Cuerpo de `POST /api/auth/verificar`. */
export interface VerificarRequest {
  /** Correo de la cuenta. */
  correo: string;
  /** Código de 6 dígitos. */
  codigo: string;
}

/** Cuerpo de `POST /api/auth/reenviar-codigo`. */
export interface ReenviarRequest {
  /** Correo de la cuenta. */
  correo: string;
}

/** Respuesta con un mensaje de texto (`MensajeResponse`). */
export interface MensajeResponse {
  /** Mensaje para el usuario. */
  mensaje: string;
}
