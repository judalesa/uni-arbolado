import { Injectable, computed, signal } from '@angular/core';

import { LoginResponse } from '../model/auth.model';
import { Rol, UsuarioDto } from '../model/usuario.model';

/** Clave con la que se guarda la sesión en `sessionStorage`. */
const CLAVE_SESION = 'arbolado.sesion';

/** Datos que se conservan de una sesión iniciada. */
interface Sesion {
  /** JWT para el encabezado `Authorization`. */
  token: string;
  /** Instante de vencimiento (UTC). */
  expiraEn: string;
  /** Usuario autenticado. */
  usuario: UsuarioDto;
}

/**
 * Guarda quién inició sesión. Usa `sessionStorage` y no `localStorage`: la sesión muere al cerrar la
 * pestaña, lo que reduce el tiempo que un token queda expuesto en un equipo compartido.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  /** Sesión actual; `null` si no hay. */
  private readonly sesion = signal<Sesion | null>(this.leer());

  /** Usuario autenticado, o `null`. */
  readonly usuario = computed(() => this.sesion()?.usuario ?? null);

  /** Rol del usuario autenticado, o `null`. */
  readonly rol = computed<Rol | null>(() => this.sesion()?.usuario.rol ?? null);

  /**
   * Guarda la sesión recibida del login.
   *
   * @param respuesta respuesta de `POST /api/auth/login`
   */
  iniciar(respuesta: LoginResponse): void {
    const sesion: Sesion = {
      token: respuesta.token,
      expiraEn: respuesta.expiraEn,
      usuario: respuesta.usuario,
    };
    this.sesion.set(sesion);
    this.escribir(sesion);
  }

  /** Borra la sesión local; el cierre en el servidor lo hace el servicio de autenticación. */
  cerrar(): void {
    this.sesion.set(null);
    this.escribir(null);
  }

  /**
   * Token vigente para enviar al backend.
   *
   * @return el token, o `null` si no hay sesión o ya venció
   */
  token(): string | null {
    return this.estaVigente() ? (this.sesion()?.token ?? null) : null;
  }

  /**
   * Indica si hay una sesión no vencida. Si venció, la borra: así el frontend no reenvía un token que
   * el backend va a rechazar.
   *
   * @return {@code true} si hay sesión vigente
   */
  estaVigente(): boolean {
    const actual = this.sesion();
    if (actual === null) {
      return false;
    }
    if (Date.parse(actual.expiraEn) <= Date.now()) {
      this.cerrar();
      return false;
    }
    return true;
  }

  /**
   * Lee la sesión guardada, tolerando almacenamiento bloqueado o contenido dañado.
   *
   * @return la sesión guardada, o `null`
   */
  private leer(): Sesion | null {
    try {
      const texto = sessionStorage.getItem(CLAVE_SESION);
      return texto === null ? null : (JSON.parse(texto) as Sesion);
    } catch {
      return null;
    }
  }

  /**
   * Guarda o borra la sesión en `sessionStorage`; si el navegador lo impide, la sesión sigue en memoria.
   *
   * @param sesion sesión a guardar, o `null` para borrar
   */
  private escribir(sesion: Sesion | null): void {
    try {
      if (sesion === null) {
        sessionStorage.removeItem(CLAVE_SESION);
      } else {
        sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
      }
    } catch {
      /* Sin almacenamiento: la sesión queda solo en memoria. */
    }
  }
}
