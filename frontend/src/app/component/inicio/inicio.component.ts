import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SesionService } from '../../service/sesion.service';
import { rutaInicial } from '../../util/rutas-por-rol.util';

/** Pantalla de bienvenida (ruta `/`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-inicio',
  /** Dependencias de la plantilla. */
  imports: [RouterLink],
  /** Plantilla HTML. */
  templateUrl: './inicio.component.html',
})
export class InicioComponent {
  /** Sesión actual, para ofrecer entrar o ir al panel. */
  private readonly sesion = inject(SesionService);

  /** Título principal. */
  protected readonly titulo = 'Sistema de Gestión Académica en Línea';

  /** Texto de apoyo bajo el título. */
  protected readonly descripcion =
    'Consulta tu plan de estudios, inscribe asignaturas y revisa tus notas.';

  /** Pantalla inicial del rol si hay sesión; `null` si no. */
  protected readonly panel = computed(() => {
    const rol = this.sesion.rol();
    return rol ? rutaInicial(rol) : null;
  });
}
