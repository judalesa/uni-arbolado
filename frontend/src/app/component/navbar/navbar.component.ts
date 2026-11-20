import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { SesionService } from '../../service/sesion.service';
import { MENU_POR_ROL } from '../../util/rutas-por-rol.util';

/** Barra de navegación: marca de la institución, menú del rol y datos de la sesión. */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-navbar',
  /** Dependencias de la plantilla. */
  imports: [RouterLink, RouterLinkActive],
  /** Plantilla HTML. */
  templateUrl: './navbar.component.html',
})
export class NavbarComponent {
  /** Sesión actual. */
  private readonly sesion = inject(SesionService);

  /** Enrutador, para volver a `/login` al salir. */
  private readonly router = inject(Router);

  /** Nombre de la institución mostrado en la marca. */
  protected readonly institucion = 'Universidad El Arbolado';

  /** Usuario autenticado, o `null`. */
  protected readonly usuario = this.sesion.usuario;

  /** Opciones del menú según el rol; vacío si no hay sesión. */
  protected readonly menu = computed(() => {
    const rol = this.sesion.rol();
    return rol ? MENU_POR_ROL[rol] : [];
  });

  /** Controla el menú desplegable en pantallas angostas. */
  protected readonly menuAbierto = signal(false);

  /** Alterna el menú desplegable. */
  protected alternarMenu(): void {
    this.menuAbierto.update((abierto) => !abierto);
  }

  /** Cierra la sesión local y vuelve al inicio de sesión; FE-02 añade el cierre en el servidor. */
  protected salir(): void {
    this.sesion.cerrar();
    this.menuAbierto.set(false);
    void this.router.navigateByUrl('/login');
  }
}
