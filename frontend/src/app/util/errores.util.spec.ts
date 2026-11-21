import { HttpErrorResponse } from '@angular/common/http';

import { MENSAJE_GENERICO, erroresPorCampo, mensajesDe } from './errores.util';

/**
 * Crea un error HTTP con el cuerpo dado.
 *
 * @param cuerpo cuerpo de la respuesta
 * @param status código HTTP
 * @return error HTTP
 */
const http = (cuerpo: unknown, status = 400): HttpErrorResponse =>
  new HttpErrorResponse({ error: cuerpo, status });

/** Cuerpo base de un error del contrato. */
const BASE = {
  timestamp: '2026-11-19T10:00:00Z',
  status: 400,
  codigo: 'X',
  mensaje: 'Mensaje del servidor',
  ruta: '/api/x',
};

/** Pruebas de los ayudantes que interpretan errores de la API. */
describe('errores', () => {
  /** Un 422 muestra todos los motivos juntos. */
  it('lista todos los motivos de un 422', () => {
    const error = http({
      ...BASE,
      status: 422,
      motivos: [
        { codigo: 'GRUPO_LLENO', mensaje: 'El grupo está lleno' },
        { codigo: 'CREDITOS_EXCEDIDOS', mensaje: 'Superaría 12 créditos' },
      ],
    });

    expect(mensajesDe(error)).toEqual(['El grupo está lleno', 'Superaría 12 créditos']);
  });

  /** Un 400 muestra el mensaje de cada campo. */
  it('lista los detalles de un 400', () => {
    const error = http({
      ...BASE,
      detalles: [{ campo: 'correo', mensaje: 'Debe ser un correo válido' }],
    });

    expect(mensajesDe(error)).toEqual(['Debe ser un correo válido']);
  });

  /** Sin motivos ni detalles se usa el mensaje del servidor. */
  it('usa el mensaje del servidor', () => {
    expect(mensajesDe(http(BASE, 409))).toEqual(['Mensaje del servidor']);
  });

  /** Sin conexión no hay cuerpo: se muestra un mensaje genérico, nunca el error técnico. */
  it('usa un mensaje genérico si no hay cuerpo de la API', () => {
    expect(mensajesDe(http(null, 0))).toEqual([MENSAJE_GENERICO]);
    expect(mensajesDe(new Error('boom'))).toEqual([MENSAJE_GENERICO]);
  });

  /** Los detalles se agrupan por campo y gana el primero si un campo falla dos veces. */
  it('agrupa los errores por campo', () => {
    const error = http({
      ...BASE,
      detalles: [
        { campo: 'documento', mensaje: 'Debe tener 10 dígitos' },
        { campo: 'documento', mensaje: 'Otro mensaje' },
        { campo: 'correo', mensaje: 'Correo no válido' },
      ],
    });

    expect(erroresPorCampo(error)).toEqual({
      documento: 'Debe tener 10 dígitos',
      correo: 'Correo no válido',
    });
  });
});
