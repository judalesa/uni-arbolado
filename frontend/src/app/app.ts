import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

/** Componente raíz: encabezado, contenido de la ruta actual y pie de página. */
@Component({
  /** Etiqueta con la que `index.html` monta el componente. */
  selector: 'app-root',
  /** Dependencias usadas en la plantilla. */
  imports: [RouterOutlet, RouterLink],
  /** Plantilla HTML. */
  templateUrl: './app.html',
  /** Estilos del componente. */
  styleUrl: './app.css',
})
export class App {
  /** Nombre de la institución mostrado en el encabezado. */
  protected readonly institucion = 'Universidad El Arbolado';

  /** Año actual para el pie de página. */
  protected readonly anio = new Date().getFullYear();
}
