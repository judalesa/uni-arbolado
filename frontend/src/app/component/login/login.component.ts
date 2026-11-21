import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../service/auth.service';
import { SesionService } from '../../service/sesion.service';
import { ToastService } from '../../service/toast.service';
import { erroresPorCampo, leerError, mensajesDe } from '../../util/errores.util';
import { rutaInicial } from '../../util/rutas-por-rol.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import {
  CampoFormulario,
  FormularioComponent,
  ValoresFormulario,
} from '../formulario/formulario.component';

/** Pantalla de inicio de sesión (`/login`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-login',
  /** Dependencias de la plantilla. */
  imports: [FormularioComponent, AlertaErroresComponent, RouterLink],
  /** Plantilla HTML. */
  templateUrl: './login.component.html',
})
export class LoginComponent {
  /** Servicio de autenticación. */
  private readonly auth = inject(AuthService);

  /** Sesión local. */
  private readonly sesion = inject(SesionService);

  /** Enrutador. */
  private readonly router = inject(Router);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Campos del formulario. */
  protected readonly campos: CampoFormulario[] = [
    {
      nombre: 'correo',
      etiqueta: 'Correo',
      tipo: 'email',
      requerido: true,
      maximo: 120,
      autocompletar: 'username',
    },
    {
      nombre: 'contrasena',
      etiqueta: 'Contraseña',
      tipo: 'password',
      requerido: true,
      maximo: 128,
      autocompletar: 'current-password',
    },
  ];

  /** Indica que se está iniciando sesión. */
  protected readonly enviando = signal(false);

  /** Mensajes de error generales. */
  protected readonly errores = signal<string[]>([]);

  /** Errores por campo devueltos por el backend. */
  protected readonly erroresCampo = signal<Record<string, string>>({});

  /**
   * Envía las credenciales y, si son correctas, lleva a la pantalla inicial del rol.
   *
   * @param valores correo y contraseña
   */
  protected ingresar(valores: ValoresFormulario): void {
    this.enviando.set(true);
    this.errores.set([]);
    this.erroresCampo.set({});
    const correo = String(valores['correo']);
    this.auth.login({ correo, contrasena: String(valores['contrasena']) }).subscribe({
      next: (respuesta) => {
        this.sesion.iniciar(respuesta);
        this.router.navigateByUrl(rutaInicial(respuesta.usuario.rol));
      },
      error: (error: unknown) => this.alFallar(error, correo),
    });
  }

  /**
   * Una cuenta pendiente se lleva a verificar; el resto de errores se muestran en la pantalla.
   *
   * @param error error recibido
   * @param correo correo con el que se intentó entrar
   */
  private alFallar(error: unknown, correo: string): void {
    this.enviando.set(false);
    if (leerError(error)?.codigo === 'CUENTA_PENDIENTE') {
      this.auth.correoPendiente.set(correo);
      this.avisos.advertencia(
        'Tu cuenta aún no está verificada. Ingresa el código que te enviamos.',
      );
      this.router.navigateByUrl('/verificar');
      return;
    }
    this.errores.set(mensajesDe(error));
    this.erroresCampo.set(erroresPorCampo(error));
  }
}
