import { LoginResponse } from '../model/auth.model';
import { Rol } from '../model/usuario.model';

/**
 * Crea una respuesta de login de ejemplo para las pruebas.
 *
 * @param rol rol del usuario
 * @param minutos minutos hasta que vence el token; negativo para un token ya vencido
 * @return respuesta de login
 */
export function loginDePrueba(rol: Rol = 'ESTUDIANTE', minutos = 60): LoginResponse {
  return {
    token: 'token-de-prueba',
    tipo: 'Bearer',
    expiraEn: new Date(Date.now() + minutos * 60_000).toISOString(),
    usuario: {
      id: 1,
      documento: '1012345678',
      nombreCompleto: 'Ana Pérez',
      correo: 'ana@example.com',
      rol,
      estado: 'ACTIVO',
    },
  };
}
