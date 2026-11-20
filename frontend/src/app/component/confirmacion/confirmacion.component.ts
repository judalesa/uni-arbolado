import { Component, input, output } from '@angular/core';

/**
 * Cuadro de confirmación con el modal de Bootstrap. Se usa donde la acción no se puede deshacer:
 * la inscripción (RN-23) y el registro de una nota (D-20). El contenido adicional va dentro de la etiqueta.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-confirmacion',
  /** Plantilla HTML. */
  templateUrl: './confirmacion.component.html',
})
export class ConfirmacionComponent {
  /** Indica si el cuadro está visible. */
  readonly abierto = input(false);

  /** Título del cuadro. */
  readonly titulo = input('Confirma la acción');

  /** Texto principal. */
  readonly mensaje = input('');

  /** Texto del botón que confirma. */
  readonly textoConfirmar = input('Confirmar');

  /** Texto del botón que cancela. */
  readonly textoCancelar = input('Cancelar');

  /** Indica que se está procesando; bloquea los botones. */
  readonly procesando = input(false);

  /** Se emite cuando el usuario confirma. */
  readonly confirmar = output<void>();

  /** Se emite cuando el usuario cancela o cierra el cuadro. */
  readonly cancelar = output<void>();
}
