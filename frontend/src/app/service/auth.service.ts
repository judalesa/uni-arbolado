import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { EMPTY, Observable, catchError, finalize } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  LoginRequest,
  LoginResponse,
  MensajeResponse,
  RegistroRequest,
  ReenviarRequest,
  VerificarRequest,
} from '../model/auth.model';
import { SesionService } from './sesion.service';

/** Llamadas a `/api/auth/*` y cierre de sesión. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Sesión local. */
  private readonly sesion = inject(SesionService);

  /** Enrutador, para volver al login al cerrar sesión. */
  private readonly router = inject(Router);

  /** Base de las rutas de autenticación. */
  private readonly base = `${environment.apiUrl}/api/auth`;

  /**
   * Correo de la cuenta que falta verificar. Pasa de la pantalla de registro o de login a la de
   * verificación en memoria: así el correo no viaja en la URL ni queda guardado en el navegador.
   */
  readonly correoPendiente = signal<string | null>(null);

  /**
   * Inicia sesión.
   *
   * @param datos correo y contraseña
   * @return el token y el usuario
   */
  login(datos: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, datos);
  }

  /**
   * Registra un estudiante; el backend envía el código de verificación al correo.
   *
   * @param datos datos del registro
   * @return mensaje de confirmación
   */
  registrar(datos: RegistroRequest): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(`${this.base}/registro`, datos);
  }

  /**
   * Verifica el correo con el código de 6 dígitos.
   *
   * @param datos correo y código
   * @return mensaje de confirmación
   */
  verificar(datos: VerificarRequest): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(`${this.base}/verificar`, datos);
  }

  /**
   * Pide un código nuevo, que invalida el anterior.
   *
   * @param datos correo de la cuenta
   * @return mensaje de confirmación
   */
  reenviar(datos: ReenviarRequest): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(`${this.base}/reenviar-codigo`, datos);
  }

  /**
   * Cierra la sesión: avisa al backend para revocar el token y siempre borra la sesión local y vuelve
   * al login, aunque la llamada falle (sin red o token ya vencido). Así el usuario nunca queda atrapado.
   */
  salir(): void {
    this.http
      .post(`${this.base}/logout`, null)
      .pipe(
        catchError(() => EMPTY),
        finalize(() => {
          this.sesion.cerrar();
          this.router.navigateByUrl('/login');
        }),
      )
      .subscribe();
  }
}
