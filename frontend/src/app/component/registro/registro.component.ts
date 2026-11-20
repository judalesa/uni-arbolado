import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../service/auth.service';
import { ToastService } from '../../service/toast.service';
import { erroresPorCampo, leerError, mensajesDe } from '../../util/errores.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import {
  CampoFormulario,
  FormularioComponent,
  ValoresFormulario,
} from '../formulario/formulario.component';

/** Campo del formulario al que corresponde cada error de duplicado del backend. */
const CAMPO_POR_CODIGO: Record<string, string> = {
  DOCUMENTO_DUPLICADO: 'documento',
  CORREO_DUPLICADO: 'correo',
};

/** Pantalla de registro de estudiante (`/registro`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-registro',
  /** Dependencias de la plantilla. */
  imports: [FormularioComponent, AlertaErroresComponent, RouterLink],
  /** Plantilla HTML. */
  templateUrl: './registro.component.html',
})
export class RegistroComponent {
  /** Servicio de autenticación. */
  private readonly auth = inject(AuthService);

  /** Enrutador. */
  private readonly router = inject(Router);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Campos del formulario; las reglas son las del contrato (`RegistroRequest`). */
  protected readonly campos: CampoFormulario[] = [
    {
      nombre: 'documento',
      etiqueta: 'Documento',
      tipo: 'text',
      requerido: true,
      patron: '^[0-9]{10}$',
      mensajePatron: 'El documento debe tener exactamente 10 dígitos.',
      marcador: '1012345678',
      autocompletar: 'off',
    },
    {
      nombre: 'nombreCompleto',
      etiqueta: 'Nombre completo',
      tipo: 'text',
      requerido: true,
      minimo: 3,
      maximo: 120,
      autocompletar: 'name',
    },
    {
      nombre: 'correo',
      etiqueta: 'Correo',
      tipo: 'email',
      requerido: true,
      maximo: 120,
      autocompletar: 'email',
      ayuda: 'Te enviaremos un código de verificación.',
    },
    {
      nombre: 'contrasena',
      etiqueta: 'Contraseña',
      tipo: 'password',
      requerido: true,
      minimo: 8,
      maximo: 72,
      autocompletar: 'new-password',
      ayuda: 'Mínimo 8 caracteres.',
    },
  ];

  /** Indica que se está enviando el registro. */
  protected readonly enviando = signal(false);

  /** Mensajes de error generales. */
  protected readonly errores = signal<string[]>([]);

  /** Errores por campo devueltos por el backend. */
  protected readonly erroresCampo = signal<Record<string, string>>({});

  /**
   * Envía el registro y, si se acepta, lleva a la verificación del código.
   *
   * @param valores datos del formulario
   */
  protected registrar(valores: ValoresFormulario): void {
    this.enviando.set(true);
    this.errores.set([]);
    this.erroresCampo.set({});
    const correo = String(valores['correo']);
    this.auth
      .registrar({
        documento: String(valores['documento']),
        nombreCompleto: String(valores['nombreCompleto']),
        correo,
        contrasena: String(valores['contrasena']),
      })
      .subscribe({
        next: (respuesta) => {
          this.auth.correoPendiente.set(correo);
          this.avisos.exito(respuesta.mensaje);
          void this.router.navigateByUrl('/verificar');
        },
        error: (error: unknown) => this.alFallar(error),
      });
  }

  /**
   * Un documento o correo repetido se marca en su campo; el resto se muestra como mensaje general.
   *
   * @param error error recibido
   */
  private alFallar(error: unknown): void {
    this.enviando.set(false);
    const cuerpo = leerError(error);
    const campo = CAMPO_POR_CODIGO[cuerpo?.codigo ?? ''];
    if (cuerpo && campo) {
      this.erroresCampo.set({ [campo]: cuerpo.mensaje });
      return;
    }
    this.errores.set(mensajesDe(error));
    this.erroresCampo.set(erroresPorCampo(error));
  }
}
