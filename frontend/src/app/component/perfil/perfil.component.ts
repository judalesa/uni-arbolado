import { Component, OnInit, inject, signal } from '@angular/core';

import { UsuarioDto } from '../../model/usuario.model';
import { UsuarioService } from '../../service/usuario.service';
import { mensajesDe } from '../../util/errores.util';
import { formatearInstante } from '../../util/fechas.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { EtiquetaEstadoComponent } from '../etiqueta-estado/etiqueta-estado.component';

/** Nombre visible de cada rol. */
const NOMBRE_ROL: Record<string, string> = {
  ESTUDIANTE: 'Estudiante',
  PROFESOR: 'Profesor',
  ADMINISTRADOR: 'Administrador',
};

/** Perfil del usuario de la sesión, de solo lectura (`/perfil`, todos los roles). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-perfil',
  /** Dependencias de la plantilla. */
  imports: [AlertaErroresComponent, EtiquetaEstadoComponent],
  /** Plantilla HTML. */
  templateUrl: './perfil.component.html',
})
export class PerfilComponent implements OnInit {
  /** Servicio de usuarios. */
  private readonly usuarios = inject(UsuarioService);

  /** Usuario de la sesión, leído del backend para mostrar datos actuales. */
  protected readonly usuario = signal<UsuarioDto | null>(null);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Nombres de los roles, para la plantilla. */
  protected readonly nombreRol = NOMBRE_ROL;

  /** Formato de la fecha de creación, para la plantilla. */
  protected readonly formatearInstante = formatearInstante;

  /** Carga los datos del usuario. */
  ngOnInit(): void {
    this.usuarios.yo().subscribe({
      next: (usuario) => this.usuario.set(usuario),
      error: (error: unknown) => this.errores.set(mensajesDe(error)),
    });
  }
}
