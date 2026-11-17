package co.edu.unbosque.arbolado.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/** Datos para crear o modificar un periodo (RF-19). */
public class PeriodoRequest {

	/** Código con formato AAAA-N, por ejemplo 2026-2. */
	@NotBlank(message = "El código es obligatorio")
	@Pattern(regexp = "^[0-9]{4}-[0-9]$", message = "Debe tener el formato AAAA-N, por ejemplo 2026-2")
	private String codigo;

	/** Fecha de inicio. */
	@NotNull(message = "La fecha de inicio es obligatoria")
	private LocalDate fechaInicio;

	/** Fecha de fin. */
	@NotNull(message = "La fecha de fin es obligatoria")
	private LocalDate fechaFin;

	/** Constructor sin argumentos que exige Jackson. */
	public PeriodoRequest() {
	}

	/** @return código del periodo */
	public String getCodigo() {
		return codigo;
	}

	/** @param codigo código del periodo */
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	/** @return fecha de inicio */
	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	/** @param fechaInicio fecha de inicio */
	public void setFechaInicio(LocalDate fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	/** @return fecha de fin */
	public LocalDate getFechaFin() {
		return fechaFin;
	}

	/** @param fechaFin fecha de fin */
	public void setFechaFin(LocalDate fechaFin) {
		this.fechaFin = fechaFin;
	}

	/**
	 * Regla cruzada: el fin debe ser posterior al inicio. Si falta alguna fecha se deja a {@code @NotNull}.
	 *
	 * @return {@code true} si el rango es válido
	 */
	@AssertTrue(message = "La fecha de fin debe ser posterior a la de inicio")
	public boolean isRangoValido() {
		return fechaInicio == null || fechaFin == null || fechaFin.isAfter(fechaInicio);
	}
}
