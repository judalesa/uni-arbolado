import { Injectable, signal } from '@angular/core';

/** Tipo de aviso; define el color del Toast. */
export type TipoAviso = 'exito' | 'error' | 'advertencia' | 'info';

/** Aviso visible en pantalla. */
export interface Aviso {
  /** Identificador interno. */
  id: number;
  /** Tipo de aviso. */
  tipo: TipoAviso;
  /** Texto para el usuario. */
  mensaje: string;
}

/** Cola de avisos mostrados con el Toast de Bootstrap (no se usa ngx-toastr: no soporta Angular 22). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  /** Avisos visibles. */
  readonly avisos = signal<Aviso[]>([]);

  /** Contador para generar identificadores. */
  private siguiente = 1;

  /**
   * Muestra un aviso y lo quita solo pasado un tiempo.
   *
   * @param tipo tipo de aviso
   * @param mensaje texto a mostrar
   * @param milisegundos tiempo en pantalla; 0 lo deja hasta que se cierre a mano
   */
  mostrar(tipo: TipoAviso, mensaje: string, milisegundos = 5000): void {
    const id = this.siguiente++;
    this.avisos.update((lista) => [...lista, { id, tipo, mensaje }]);
    if (milisegundos > 0) {
      setTimeout(() => this.cerrar(id), milisegundos);
    }
  }

  /**
   * Muestra un aviso de éxito.
   *
   * @param mensaje texto a mostrar
   */
  exito(mensaje: string): void {
    this.mostrar('exito', mensaje);
  }

  /**
   * Muestra un aviso de error.
   *
   * @param mensaje texto a mostrar
   */
  error(mensaje: string): void {
    this.mostrar('error', mensaje, 8000);
  }

  /**
   * Muestra una advertencia.
   *
   * @param mensaje texto a mostrar
   */
  advertencia(mensaje: string): void {
    this.mostrar('advertencia', mensaje);
  }

  /**
   * Quita un aviso.
   *
   * @param id identificador del aviso
   */
  cerrar(id: number): void {
    this.avisos.update((lista) => lista.filter((aviso) => aviso.id !== id));
  }
}
