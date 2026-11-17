package co.edu.unbosque.arbolado.dto;

import java.time.LocalDate;

/** Datos de un periodo académico. */
public class PeriodoDto {

	/** Identificador. */
	private Long id;

	/** Código con formato AAAA-N. */
	private String codigo;

	/** Fecha de inicio. */
	private LocalDate fechaInicio;

	/** Fecha de fin. */
	private LocalDate fechaFin;

	/** Verdadero si es el periodo vigente. */
	private boolean activo;

	/** Constructor sin argumentos que exige ModelMapper. */
	public PeriodoDto() {
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @param id identificador */
	public void setId(Long id) {
		this.id = id;
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

	/** @return {@code true} si es el periodo vigente */
	public boolean isActivo() {
		return activo;
	}

	/** @param activo {@code true} si es el periodo vigente */
	public void setActivo(boolean activo) {
		this.activo = activo;
	}
}
