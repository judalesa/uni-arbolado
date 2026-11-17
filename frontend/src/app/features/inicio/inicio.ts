import { Component } from '@angular/core';

/**
 * Pantalla de bienvenida (ruta `/`).
 *
 * Es un marcador del esqueleto: se reemplaza por la pantalla de inicio de sesión cuando se construya la
 * funcionalidad de autenticación. Demuestra el patrón que seguirán todas las pantallas: componente
 * independiente cargado de forma diferida desde `app.routes.ts`.
 */
@Component({
  /** Etiqueta del componente (solo se usa si otro componente lo incluye directamente). */
  selector: 'app-inicio',
  /** Archivo de plantilla HTML. */
  templateUrl: './inicio.html',
})
export class Inicio {
  /** Título principal de la pantalla. */
  protected readonly titulo = 'Sistema de Gestión Académica en Línea';

  /** Texto de apoyo bajo el título. */
  protected readonly descripcion = 'Consulta tu plan de estudios, inscribe asignaturas y revisa tus notas.';
}
