import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/**
 * Componente raíz de la aplicación: dibuja el encabezado y el pie de página, y deja el espacio central
 * (`<router-outlet>`) donde Angular muestra la pantalla que corresponda a la ruta actual.
 *
 * Es un componente independiente (standalone): declara aquí mismo lo que usa en su plantilla, sin módulos.
 */
@Component({
  /** Etiqueta con la que `index.html` monta este componente. */
  selector: 'app-root',
  /** Directivas que la plantilla necesita: el enlace del logo y el punto de salida de las rutas. */
  imports: [RouterOutlet, RouterLink],
  /** Archivo de plantilla HTML. */
  templateUrl: './app.html',
  /** Archivo de estilos propios del componente. */
  styleUrl: './app.css',
})
export class App {
  /** Nombre de la institución que se muestra en el encabezado. */
  protected readonly institucion = 'Universidad El Arbolado';

  /** Año actual, mostrado en el pie de página. */
  protected readonly anio = new Date().getFullYear();
}
