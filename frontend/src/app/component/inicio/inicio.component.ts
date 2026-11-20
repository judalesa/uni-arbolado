import { Component } from '@angular/core';

/** Pantalla de bienvenida (ruta `/`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-inicio',
  /** Plantilla HTML. */
  templateUrl: './inicio.component.html',
})
export class InicioComponent {
  /** Título principal. */
  protected readonly titulo = 'Sistema de Gestión Académica en Línea';

  /** Texto de apoyo bajo el título. */
  protected readonly descripcion =
    'Consulta tu plan de estudios, inscribe asignaturas y revisa tus notas.';
}
