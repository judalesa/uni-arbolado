/**
 * Configuración del ambiente de PRODUCCIÓN (la que usa `ng build` por defecto y la que publica Netlify).
 *
 * Por qué existe: la URL del backend NUNCA se escribe dentro de un servicio. Así el mismo código sirve para
 * desarrollo y producción, y cambiar de servidor no obliga a tocar la lógica de la aplicación.
 *
 * Aquí solo van valores públicos. Una página web se descarga completa en el navegador, así que
 * ninguna contraseña, llave ni token puede vivir en estos archivos.
 */
export const environment = {
  /** Indica que es el ambiente de producción. */
  production: true,
  /** URL base del backend desplegado en el Tomcat del profesor (incluye el contexto del WAR, sin barra final). */
  apiUrl: 'https://gpcueb.org/uni-arbolado-back',
};
