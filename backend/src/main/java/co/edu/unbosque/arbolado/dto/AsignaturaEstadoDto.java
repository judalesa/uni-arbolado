package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.model.EstadoAsignatura;

import java.math.BigDecimal;

/**
 * Una asignatura del plan con su estado para el estudiante (esquema {@code AsignaturaEstadoDto} del contrato).
 *
 * @param asignatura     asignatura con sus prerrequisitos
 * @param estado         situación del estudiante en ella
 * @param notaDefinitiva definitiva del intento que define el estado, o {@code null}
 */
public record AsignaturaEstadoDto(
		AsignaturaDto asignatura,
		EstadoAsignatura estado,
		BigDecimal notaDefinitiva) {
}
