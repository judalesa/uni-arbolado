/** Página de resultados de un listado paginado (`Pagina…` del contrato). */
export interface Pagina<T> {
  /** Elementos de la página. */
  contenido: T[];
  /** Número de página, desde 0. */
  pagina: number;
  /** Elementos por página. */
  tamano: number;
  /** Total de elementos en todas las páginas. */
  totalElementos: number;
  /** Total de páginas. */
  totalPaginas: number;
}
