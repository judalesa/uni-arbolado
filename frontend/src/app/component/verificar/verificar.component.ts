import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../service/auth.service';
import { ToastService } from '../../service/toast.service';
import { mensajesDe } from '../../util/errores.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import {
  CampoFormulario,
  FormularioComponent,
  ValoresFormulario,
} from '../formulario/formulario.component';

/** Segundos entre un reenvío de código y el siguiente (propuesta P-02 del contrato). */
export const ESPERA_REENVIO = 60;

/** Pantalla para ingresar el código de verificación y pedir uno nuevo (`/verificar`). */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-verificar',
  /** Dependencias de la plantilla. */
  imports: [FormularioComponent, AlertaErroresComponent, RouterLink],
  /** Plantilla HTML. */
  templateUrl: './verificar.component.html',
})
export class VerificarComponent {
  /** Servicio de autenticación. */
  private readonly auth = inject(AuthService);

  /** Enrutador. */
  private readonly router = inject(Router);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Temporizador de la espera entre reenvíos. */
  private temporizador: ReturnType<typeof setInterval> | null = null;

  /** Correo de la cuenta a verificar; viene del registro o del login, o se pide aquí. */
  protected readonly correo = signal<string | null>(this.auth.correoPendiente());

  /** Campo para pedir el correo cuando no se conoce. */
  protected readonly camposCorreo: CampoFormulario[] = [
    {
      nombre: 'correo',
      etiqueta: 'Correo de tu cuenta',
      tipo: 'email',
      requerido: true,
      maximo: 120,
    },
  ];

  /** Campo del código de 6 dígitos. */
  protected readonly camposCodigo: CampoFormulario[] = [
    {
      nombre: 'codigo',
      etiqueta: 'Código de verificación',
      tipo: 'text',
      requerido: true,
      patron: '^[0-9]{6}$',
      mensajePatron: 'El código son 6 dígitos.',
      marcador: '123456',
      autocompletar: 'one-time-code',
    },
  ];

  /** Indica que se está verificando. */
  protected readonly enviando = signal(false);

  /** Indica que se está reenviando el código. */
  protected readonly reenviando = signal(false);

  /** Segundos que faltan para poder reenviar; 0 si ya se puede. */
  protected readonly espera = signal(0);

  /** Mensajes de error generales. */
  protected readonly errores = signal<string[]>([]);

  /** Texto del botón de reenvío, con la cuenta regresiva si corresponde. */
  protected readonly textoReenviar = computed(() =>
    this.espera() > 0 ? `Reenviar código (${this.espera()} s)` : 'Reenviar código',
  );

  /** Detiene el temporizador al destruir la pantalla. */
  constructor() {
    inject(DestroyRef).onDestroy(() => this.detenerTemporizador());
  }

  /**
   * Guarda el correo escrito y pasa a pedir el código.
   *
   * @param valores contiene el correo
   */
  protected continuar(valores: ValoresFormulario): void {
    this.errores.set([]);
    this.correo.set(String(valores['correo']));
  }

  /** Vuelve a pedir el correo, por si se escribió mal. */
  protected cambiarCorreo(): void {
    this.errores.set([]);
    this.correo.set(null);
  }

  /**
   * Verifica el código; si es correcto, activa la cuenta y lleva al login.
   *
   * @param valores contiene el código
   */
  protected verificar(valores: ValoresFormulario): void {
    this.enviando.set(true);
    this.errores.set([]);
    this.auth
      .verificar({ correo: this.correo() ?? '', codigo: String(valores['codigo']) })
      .subscribe({
        next: (respuesta) => {
          this.auth.correoPendiente.set(null);
          this.avisos.exito(respuesta.mensaje);
          this.router.navigateByUrl('/login');
        },
        error: (error: unknown) => {
          this.enviando.set(false);
          this.errores.set(mensajesDe(error));
        },
      });
  }

  /** Pide un código nuevo, que invalida el anterior, y arranca la espera para el siguiente. */
  protected reenviar(): void {
    this.reenviando.set(true);
    this.errores.set([]);
    this.auth.reenviar({ correo: this.correo() ?? '' }).subscribe({
      next: (respuesta) => {
        this.reenviando.set(false);
        this.avisos.exito(respuesta.mensaje);
        this.iniciarEspera();
      },
      error: (error: unknown) => {
        this.reenviando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }

  /** Arranca la cuenta regresiva del reenvío. */
  private iniciarEspera(): void {
    this.detenerTemporizador();
    this.espera.set(ESPERA_REENVIO);
    this.temporizador = setInterval(() => {
      this.espera.update((segundos) => segundos - 1);
      if (this.espera() <= 0) {
        this.detenerTemporizador();
      }
    }, 1000);
  }

  /** Detiene el temporizador si está corriendo. */
  private detenerTemporizador(): void {
    if (this.temporizador !== null) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }
}
