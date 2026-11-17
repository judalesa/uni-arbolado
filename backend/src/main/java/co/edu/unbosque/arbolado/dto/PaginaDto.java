package co.edu.unbosque.arbolado.dto;

import java.util.List;

/**
 * Página de resultados con la forma del contrato ({@code PaginaUsuario} y las páginas futuras).
 *
 * @param contenido      elementos de la página
 * @param pagina         número de página, desde 0
 * @param tamano         elementos pedidos por página
 * @param totalElementos total de elementos que cumplen el filtro
 * @param totalPaginas   total de páginas
 * @param <T>            tipo de los elementos
 */
public record PaginaDto<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {
}
