package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.exception.CodigoMotivo;

/**
 * Uno de los incumplimientos de una regla de negocio (solo aparece en respuestas 422).
 *
 * <p>Una inscripción rechazada puede traer varios motivos a la vez (RN-22): el estudiante ve la lista
 * completa y no tiene que descubrirlos uno por uno.</p>
 *
 * @param codigo  código estable del motivo, por ejemplo {@code CREDITOS_EXCEDIDOS}
 * @param mensaje explicación legible para el estudiante
 */
public record Motivo(String codigo, String mensaje) {

	/**
	 * Crea un motivo a partir del catálogo de códigos, evitando errores de digitación.
	 *
	 * @param codigo  código del catálogo
	 * @param mensaje explicación legible
	 * @return el motivo creado
	 */
	public static Motivo de(CodigoMotivo codigo, String mensaje) {
		return new Motivo(codigo.name(), mensaje);
	}
}
