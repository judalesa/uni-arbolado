package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.exception.CodigoError;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de TODA respuesta de error de la API (esquema {@code ErrorResponse} del contrato OpenAPI).
 *
 * <p>El frontend decide qué mostrar con {@link #codigo()}, que es estable; el {@link #mensaje()} es
 * solo texto para personas. Las listas {@link #detalles()} y {@link #motivos()} solo se serializan
 * cuando existen.</p>
 *
 * @param timestamp instante del error, en UTC
 * @param status    código HTTP
 * @param codigo    código estable del error (ver {@link CodigoError})
 * @param mensaje   explicación legible
 * @param ruta      ruta solicitada, sin el contexto de despliegue
 * @param detalles  errores por campo (solo en 400)
 * @param motivos   incumplimientos de la regla de negocio (solo en 422)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		Instant timestamp,
		int status,
		String codigo,
		String mensaje,
		String ruta,
		List<ErrorCampo> detalles,
		List<Motivo> motivos) {

	/**
	 * Crea una respuesta de error simple, con la hora actual.
	 *
	 * @param codigo  código estable; de él se toma el estado HTTP
	 * @param mensaje explicación legible
	 * @param ruta    ruta solicitada
	 * @return la respuesta sin detalles ni motivos
	 */
	public static ErrorResponse de(CodigoError codigo, String mensaje, String ruta) {
		return new ErrorResponse(Instant.now(), codigo.estado(), codigo.name(), mensaje, ruta, null, null);
	}

	/**
	 * Copia esta respuesta agregando los errores por campo.
	 *
	 * @param lista errores de validación
	 * @return una nueva respuesta con {@code detalles}
	 */
	public ErrorResponse conDetalles(List<ErrorCampo> lista) {
		return new ErrorResponse(timestamp, status, codigo, mensaje, ruta, List.copyOf(lista), motivos);
	}

	/**
	 * Copia esta respuesta agregando los motivos de rechazo.
	 *
	 * @param lista motivos de la regla incumplida
	 * @return una nueva respuesta con {@code motivos}
	 */
	public ErrorResponse conMotivos(List<Motivo> lista) {
		return new ErrorResponse(timestamp, status, codigo, mensaje, ruta, detalles, List.copyOf(lista));
	}
}
