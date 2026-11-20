package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.model.DiaSemana;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Franja semanal de un grupo, para pedir y responder. Las horas viajan como texto HH:mm. */
public class HorarioDto {

	/** Patrón de una hora de 00:00 a 23:59. */
	private static final String HORA = "^([01][0-9]|2[0-3]):[0-5][0-9]$";

	/** Día de la semana. */
	@NotNull(message = "El día es obligatorio")
	private DiaSemana diaSemana;

	/** Hora de inicio con formato HH:mm. */
	@NotBlank(message = "La hora de inicio es obligatoria")
	@Pattern(regexp = HORA, message = "Debe tener el formato HH:mm, por ejemplo 08:00")
	private String horaInicio;

	/** Hora de fin con formato HH:mm. */
	@NotBlank(message = "La hora de fin es obligatoria")
	@Pattern(regexp = HORA, message = "Debe tener el formato HH:mm, por ejemplo 10:00")
	private String horaFin;

	/** Constructor sin argumentos que exige Jackson. */
	public HorarioDto() {
	}

	/**
	 * Crea una franja.
	 *
	 * @param diaSemana  día de la semana
	 * @param horaInicio hora de inicio HH:mm
	 * @param horaFin    hora de fin HH:mm
	 */
	public HorarioDto(DiaSemana diaSemana, String horaInicio, String horaFin) {
		this.diaSemana = diaSemana;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}

	/** @return día de la semana */
	public DiaSemana getDiaSemana() {
		return diaSemana;
	}

	/** @param diaSemana día de la semana */
	public void setDiaSemana(DiaSemana diaSemana) {
		this.diaSemana = diaSemana;
	}

	/** @return hora de inicio HH:mm */
	public String getHoraInicio() {
		return horaInicio;
	}

	/** @param horaInicio hora de inicio HH:mm */
	public void setHoraInicio(String horaInicio) {
		this.horaInicio = horaInicio;
	}

	/** @return hora de fin HH:mm */
	public String getHoraFin() {
		return horaFin;
	}

	/** @param horaFin hora de fin HH:mm */
	public void setHoraFin(String horaFin) {
		this.horaFin = horaFin;
	}
}
