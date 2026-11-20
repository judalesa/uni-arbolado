/**
 * Muestra una fecha sin hora (`AAAA-MM-DD`) como `DD/MM/AAAA`. Se arma a mano y no con `Date` porque
 * `new Date('2026-01-19')` se interpreta en UTC y en Colombia mostraría el día anterior.
 *
 * @param fecha fecha del backend
 * @return fecha legible, o cadena vacía si no hay fecha
 */
export const formatearFecha = (fecha: string | null | undefined): string => {
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(fecha ?? '');
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : '';
};

/**
 * Muestra un instante UTC en la hora local del navegador.
 *
 * @param instante instante del backend (termina en `Z`)
 * @return fecha y hora locales, o cadena vacía si no hay instante
 */
export const formatearInstante = (instante: string | null | undefined): string => {
  if (!instante) {
    return '';
  }
  const fecha = new Date(instante);
  return Number.isNaN(fecha.getTime())
    ? ''
    : fecha.toLocaleString('es-CO', { dateStyle: 'short', timeStyle: 'short' });
};
