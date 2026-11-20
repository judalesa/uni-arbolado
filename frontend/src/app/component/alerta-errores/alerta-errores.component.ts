import { Component, input } from '@angular/core';

/** Cuadro rojo con una lista de mensajes de error; no se dibuja si no hay mensajes. */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-alerta-errores',
  /** Plantilla HTML. */
  templateUrl: './alerta-errores.component.html',
})
export class AlertaErroresComponent {
  /** Mensajes a mostrar. */
  readonly mensajes = input<string[]>([]);
}
