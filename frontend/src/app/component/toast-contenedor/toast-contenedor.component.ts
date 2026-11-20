import { Component, inject } from '@angular/core';

import { TipoAviso, ToastService } from '../../service/toast.service';

/** Clase de color de Bootstrap para cada tipo de aviso. */
const CLASES: Record<TipoAviso, string> = {
  exito: 'text-bg-success',
  error: 'text-bg-danger',
  advertencia: 'text-bg-warning',
  info: 'text-bg-info',
};

/** Dibuja los avisos del {@link ToastService} en la esquina superior derecha. */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-toast-contenedor',
  /** Plantilla HTML. */
  templateUrl: './toast-contenedor.component.html',
})
export class ToastContenedorComponent {
  /** Servicio con la cola de avisos. */
  protected readonly toast = inject(ToastService);

  /** Clase de color de cada tipo de aviso, consultada desde la plantilla. */
  protected readonly clases = CLASES;
}
