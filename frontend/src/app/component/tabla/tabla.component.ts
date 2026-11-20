import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, contentChild, input, output } from '@angular/core';

/** Definición de una columna de la tabla. */
export interface ColumnaTabla<T> {
  /** Propiedad de la fila que se muestra, si no se da `valor`. */
  clave?: string;
  /** Encabezado de la columna. */
  titulo: string;
  /** Calcula el texto de la celda cuando no basta con una propiedad. */
  valor?: (fila: T) => string | number | null | undefined;
  /** Clase CSS opcional para la celda (por ejemplo `text-end`). */
  clase?: string;
}

/** Datos de paginación que muestra el pie de la tabla. */
export interface PaginacionTabla {
  /** Página actual, desde 0. */
  pagina: number;
  /** Total de páginas. */
  totalPaginas: number;
  /** Total de elementos. */
  totalElementos: number;
}

/** Contexto que recibe la plantilla de acciones por fila. */
export interface ContextoFila<T> {
  /** Fila a la que pertenecen las acciones. */
  $implicit: T;
}

/**
 * Tabla genérica para todos los listados: columnas por configuración, estado de carga, mensaje de
 * vacío, paginación opcional y una columna de acciones que cada pantalla rellena con su propia plantilla.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-tabla',
  /** Dependencias de la plantilla. */
  imports: [NgTemplateOutlet],
  /** Plantilla HTML. */
  templateUrl: './tabla.component.html',
})
export class TablaComponent<T extends object> {
  /** Columnas a mostrar. */
  readonly columnas = input.required<ColumnaTabla<T>[]>();

  /** Filas a mostrar. */
  readonly filas = input.required<T[]>();

  /** Indica que se están cargando los datos. */
  readonly cargando = input(false);

  /** Texto cuando no hay filas. */
  readonly vacio = input('No hay registros para mostrar.');

  /** Paginación; si es `null`, no se muestra el pie. */
  readonly paginacion = input<PaginacionTabla | null>(null);

  /** Encabezado de la columna de acciones. */
  readonly tituloAcciones = input('Acciones');

  /** Emite el número de página pedido (desde 0). */
  readonly cambioPagina = output<number>();

  /** Plantilla de acciones por fila, declarada por la pantalla con `#acciones`. */
  protected readonly acciones = contentChild<TemplateRef<ContextoFila<T>>>('acciones');

  /**
   * Texto de una celda; un valor vacío se muestra como guion.
   *
   * @param fila fila actual
   * @param columna columna actual
   * @return texto a mostrar
   */
  protected celda(fila: T, columna: ColumnaTabla<T>): string {
    const valor = columna.valor
      ? columna.valor(fila)
      : columna.clave
        ? (fila as Record<string, unknown>)[columna.clave]
        : undefined;
    return valor === null || valor === undefined || valor === '' ? '—' : String(valor);
  }

  /**
   * Pide otra página si existe.
   *
   * @param pagina número de página destino
   */
  protected ir(pagina: number): void {
    const datos = this.paginacion();
    if (datos && pagina >= 0 && pagina < datos.totalPaginas) {
      this.cambioPagina.emit(pagina);
    }
  }
}
