/**
 * Configuración del ambiente de DESARROLLO (la que usa `ng serve`).
 *
 * Angular reemplaza `environment.ts` por este archivo cuando se compila con la configuración
 * `development` (ver `fileReplacements` en `angular.json`).
 */
export const environment = {
  /** Indica que NO es producción. */
  production: false,
  /** URL del backend ejecutado en el computador del desarrollador con `./mvnw spring-boot:run`. */
  apiUrl: 'http://localhost:8080',
};
