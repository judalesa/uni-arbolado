import { HttpErrorResponse } from '@angular/common/http';

import { ErrorResponse } from '../model/error.model';

/** Mensaje cuando no hay nada más específico que decir. */
export const MENSAJE_GENERICO = 'No se pudo completar la operación. Inténtalo de nuevo.';

/**
 * Extrae el `ErrorResponse` del contrato de un error HTTP.
 *
 * @param error error capturado
 * @return el cuerpo del error, o `null` si no es una respuesta de la API (por ejemplo sin conexión)
 */
export function leerError(error: unknown): ErrorResponse | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const cuerpo = error.error as Partial<ErrorResponse>;
    if (typeof cuerpo.codigo === 'string' && typeof cuerpo.mensaje === 'string') {
      return cuerpo as ErrorResponse;
    }
  }
  return null;
}

/**
 * Convierte un error en la lista de mensajes que ve el usuario. Un 422 trae todos los motivos y se
 * muestran juntos (RN-22); un 400 trae un detalle por campo; el resto, el mensaje del servidor.
 *
 * @param error error capturado
 * @return mensajes en español, al menos uno
 */
export function mensajesDe(error: unknown): string[] {
  const cuerpo = leerError(error);
  if (cuerpo === null) {
    return [MENSAJE_GENERICO];
  }
  if (cuerpo.motivos && cuerpo.motivos.length > 0) {
    return cuerpo.motivos.map((motivo) => motivo.mensaje);
  }
  if (cuerpo.detalles && cuerpo.detalles.length > 0) {
    return cuerpo.detalles.map((detalle) => detalle.mensaje);
  }
  return [cuerpo.mensaje];
}

/**
 * Agrupa los errores de validación (400) por nombre de campo para mostrarlos junto a cada campo.
 *
 * @param error error capturado
 * @return mapa campo → mensaje; vacío si el error no trae detalles
 */
export function erroresPorCampo(error: unknown): Record<string, string> {
  const resultado: Record<string, string> = {};
  for (const detalle of leerError(error)?.detalles ?? []) {
    resultado[detalle.campo] ??= detalle.mensaje;
  }
  return resultado;
}
