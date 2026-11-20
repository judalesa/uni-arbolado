import { LoginResponse } from '../model/auth.model';
import { Rol } from '../model/usuario.model';

/**
 * Crea una respuesta de login de ejemplo para las pruebas.
 *
 * @param rol rol del usuario
 * @param minutos minutos hasta que vence el token; negativo para un token ya vencido
 * @return respuesta de login
 */
export const loginDePrueba = (rol: Rol = 'ESTUDIANTE', minutos = 60): LoginResponse => ({
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
});

/**
 * Escribe un valor en un campo del formulario genérico, como lo haría el usuario.
 *
 * @param raiz elemento que contiene el formulario
 * @param nombre nombre del campo (el `id` es `campo-<nombre>`)
 * @param valor texto a escribir
 */
export const escribir = (raiz: HTMLElement, nombre: string, valor: string): void => {
  const campo = raiz.querySelector<HTMLInputElement>(`#campo-${nombre}`);
  if (!campo) {
    throw new Error(`No existe el campo ${nombre}`);
  }
  campo.value = valor;
  campo.dispatchEvent(new Event('input'));
};

/**
 * Envía el formulario, como al pulsar el botón principal.
 *
 * @param raiz elemento que contiene el formulario
 */
export const enviarFormulario = (raiz: HTMLElement): void => {
  raiz.querySelector('form')?.dispatchEvent(new Event('submit'));
};

/**
 * Cuerpo de error con el formato del contrato.
 *
 * @param status código HTTP
 * @param codigo código estable del error
 * @param mensaje mensaje en español
 * @return cuerpo de error
 */
export const errorDePrueba = (status: number, codigo: string, mensaje = 'Mensaje de error') => ({
  timestamp: '2026-11-19T10:00:00Z',
  status,
  codigo,
  mensaje,
  ruta: '/api/x',
});
