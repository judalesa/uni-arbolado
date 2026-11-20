import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

/** Pantalla de error genérica; el título y el mensaje salen de los datos de la ruta (`/403`, `/404`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-pagina-error',
  /** Dependencias de la plantilla. */
  imports: [RouterLink],
  /** Plantilla HTML. */
  templateUrl: './pagina-error.component.html',
})
export class PaginaErrorComponent {
  /** Datos de la ruta actual. */
  private readonly ruta = inject(ActivatedRoute);

  /** Código mostrado (403, 404). */
  protected readonly codigo = String(this.ruta.snapshot.data['codigo'] ?? '');

  /** Título del error. */
  protected readonly titulo = String(this.ruta.snapshot.data['titulo'] ?? 'Ocurrió un problema');

  /** Explicación breve. */
  protected readonly mensaje = String(this.ruta.snapshot.data['mensaje'] ?? '');
}
