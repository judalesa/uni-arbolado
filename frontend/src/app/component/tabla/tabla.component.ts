import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, computed, contentChild, input, output } from '@angular/core';

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

/** Fila lista para dibujar: la fila original y el texto de cada celda. */
interface FilaVista<T> {
  /** Fila original, que recibe la plantilla de acciones. */
  fila: T;
  /** Celdas ya formateadas. */
  celdas: { texto: string; clase: string }[];
}

/** Datos ya calculados del pie de paginación. */
interface PieVista {
  /** Página actual, desde 0. */
  pagina: number;
  /** Texto del pie. */
  texto: string;
  /** Si hay página anterior. */
  hayAnterior: boolean;
  /** Si hay página siguiente. */
  haySiguiente: boolean;
}

/**
 * Valor crudo de una celda: lo calcula `valor`; si no, se toma la propiedad indicada por `clave`.
 *
 * @param fila fila actual
 * @param columna columna actual
 * @return valor sin formatear
 */
const valorDeCelda = <T extends object>(fila: T, columna: ColumnaTabla<T>): unknown => {
  if (columna.valor) {
    return columna.valor(fila);
  }
  return columna.clave ? (fila as Record<string, unknown>)[columna.clave] : undefined;
};

/**
 * Texto de una celda; un valor vacío se muestra como guion.
 *
 * @param fila fila actual
 * @param columna columna actual
 * @return texto a mostrar
 */
const textoDeCelda = <T extends object>(fila: T, columna: ColumnaTabla<T>): string => {
  const valor = valorDeCelda(fila, columna);
  return valor === null || valor === undefined || valor === '' ? '—' : String(valor);
};

/**
 * Tabla genérica para todos los listados: columnas por configuración, estado de carga, mensaje de
 * vacío, paginación opcional y una columna de acciones que cada pantalla rellena con su propia plantilla.
 * Toda la lógica de presentación se calcula aquí para que la plantilla solo dibuje.
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

  /** Encabezados con su clase ya resuelta. */
  protected readonly encabezados = computed(() =>
    this.columnas().map((columna) => ({ titulo: columna.titulo, clase: columna.clase ?? '' })),
  );

  /** Cantidad de columnas, incluida la de acciones; sirve para el `colspan` de los mensajes. */
  protected readonly totalColumnas = computed(
    () => this.columnas().length + (this.acciones() ? 1 : 0),
  );

  /** Filas con el texto de cada celda ya calculado. */
  protected readonly filasVista = computed<FilaVista<T>[]>(() =>
    this.filas().map((fila) => ({
      fila,
      celdas: this.columnas().map((columna) => ({
        texto: textoDeCelda(fila, columna),
        clase: columna.clase ?? '',
      })),
    })),
  );

  /** Datos del pie de paginación; `null` si no hay paginación o todo cabe en una página. */
  protected readonly pie = computed<PieVista | null>(() => {
    const datos = this.paginacion();
    if (!datos || datos.totalPaginas <= 1) {
      return null;
    }
    return {
      pagina: datos.pagina,
      texto: `Página ${datos.pagina + 1} de ${datos.totalPaginas} · ${datos.totalElementos} registros`,
      hayAnterior: datos.pagina > 0,
      haySiguiente: datos.pagina < datos.totalPaginas - 1,
    };
  });

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
