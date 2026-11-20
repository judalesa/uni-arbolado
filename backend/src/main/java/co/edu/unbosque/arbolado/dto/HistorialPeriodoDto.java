package co.edu.unbosque.arbolado.dto;

import java.util.List;

/**
 * Intentos de un estudiante en un periodo (esquema {@code HistorialPeriodoDto} del contrato).
 *
 * @param periodo       periodo
 * @param inscripciones inscripciones del periodo con sus notas
 */
public record HistorialPeriodoDto(
		PeriodoDto periodo,
		List<InscripcionDto> inscripciones) {

	/**
	 * Copia la lista para que el registro no se pueda modificar desde fuera.
	 *
	 * @param periodo       periodo
	 * @param inscripciones inscripciones del periodo
	 */
	public HistorialPeriodoDto {
		inscripciones = List.copyOf(inscripciones);
	}
}
