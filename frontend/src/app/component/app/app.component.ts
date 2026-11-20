import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavbarComponent } from '../navbar/navbar.component';
import { ToastContenedorComponent } from '../toast-contenedor/toast-contenedor.component';

/** Componente raíz: barra de navegación, avisos, contenido de la ruta actual y pie de página. */
@Component({
  /** Etiqueta con la que `index.html` monta el componente. */
  selector: 'app-root',
  /** Dependencias usadas en la plantilla. */
  imports: [RouterOutlet, NavbarComponent, ToastContenedorComponent],
  /** Plantilla HTML. */
  templateUrl: './app.component.html',
  /** Estilos del componente. */
  styleUrl: './app.component.css',
})
export class AppComponent {
  /** Nombre de la institución mostrado en el pie de página. */
  protected readonly institucion = 'Universidad El Arbolado';

  /** Año actual para el pie de página. */
  protected readonly anio = new Date().getFullYear();
}
