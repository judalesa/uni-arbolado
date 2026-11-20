package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.model.DiaSemana;

/**
 * Una franja del horario semanal (esquema {@code ClaseDto} del contrato). Una fila por franja facilita
 * dibujar el horario como cuadrícula de días y horas.
 *
 * @param asignatura  asignatura que se dicta
 * @param grupoId     grupo de la franja
 * @param grupoNumero número del grupo
 * @param bloque      bloque del salón
 * @param salon       salón
 * @param diaSemana   día de la semana
 * @param horaInicio  hora de inicio HH:mm
 * @param horaFin     hora de fin HH:mm
 */
public record ClaseDto(
		AsignaturaResumenDto asignatura,
		Long grupoId,
		int grupoNumero,
		String bloque,
		String salon,
		DiaSemana diaSemana,
		String horaInicio,
		String horaFin) {
}
