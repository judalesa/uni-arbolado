import { HttpErrorResponse } from '@angular/common/http';

import { ErrorResponse } from '../model/error.model';

/** Mensaje cuando no hay nada más específico que decir. */
export const MENSAJE_GENERICO = 'No se pudo completar la operación. Inténtalo de nuevo.';

/**
 * Indica si un valor tiene la forma de `ErrorResponse`: un objeto con `codigo` y `mensaje` de texto.
 *
 * @param valor cuerpo de la respuesta
 * @return {@code true} si es un error de la API
 */
const esCuerpoDeApi = (valor: unknown): valor is ErrorResponse => {
  const cuerpo = valor as Partial<ErrorResponse> | null;
  return typeof cuerpo?.codigo === 'string' && typeof cuerpo.mensaje === 'string';
};

/**
 * Extrae el `ErrorResponse` del contrato de un error HTTP.
 *
 * @param error error capturado
 * @return el cuerpo del error, o `null` si no es una respuesta de la API (por ejemplo sin conexión)
 */
export const leerError = (error: unknown): ErrorResponse | null => {
  const cuerpo = error instanceof HttpErrorResponse ? error.error : null;
  return esCuerpoDeApi(cuerpo) ? cuerpo : null;
};

/**
 * Extrae los textos de una lista de motivos o detalles.
 *
 * @param lista elementos con un campo `mensaje`, o `undefined`
 * @return los mensajes; vacío si no hay lista
 */
const textosDe = (lista: { mensaje: string }[] | undefined): string[] =>
  (lista ?? []).map((elemento) => elemento.mensaje);

/**
 * Convierte un error en la lista de mensajes que ve el usuario. Un 422 trae todos los motivos y se
 * muestran juntos (RN-22); un 400 trae un detalle por campo; el resto, el mensaje del servidor.
 *
 * @param error error capturado
 * @return mensajes en español, al menos uno
 */
export const mensajesDe = (error: unknown): string[] => {
  const cuerpo = leerError(error);
  if (cuerpo === null) {
    return [MENSAJE_GENERICO];
  }
  const especificos = [textosDe(cuerpo.motivos), textosDe(cuerpo.detalles)].find(
    (lista) => lista.length > 0,
  );
  return especificos ?? [cuerpo.mensaje];
};

/**
 * Agrupa los errores de validación (400) por nombre de campo para mostrarlos junto a cada campo.
 *
 * @param error error capturado
 * @return mapa campo → mensaje; vacío si el error no trae detalles
 */
export const erroresPorCampo = (error: unknown): Record<string, string> => {
  const resultado: Record<string, string> = {};
  for (const detalle of leerError(error)?.detalles ?? []) {
    resultado[detalle.campo] ??= detalle.mensaje;
  }
  return resultado;
};
