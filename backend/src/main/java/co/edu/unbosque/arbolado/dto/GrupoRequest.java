package co.edu.unbosque.arbolado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** Datos para crear o editar un grupo (RF-21 a RF-23). El cupo no se pide: siempre es 30. */
public class GrupoRequest {

	/** Asignatura que se ofrece. */
	@NotNull(message = "La asignatura es obligatoria")
	private Long asignaturaId;

	/** Periodo de la oferta. */
	@NotNull(message = "El periodo es obligatorio")
	private Long periodoId;

	/** Profesor responsable. */
	@NotNull(message = "El profesor es obligatorio")
	private Long profesorId;

	/** Bloque del edificio. */
	@NotBlank(message = "El bloque es obligatorio")
	@Size(max = 20, message = "No puede superar 20 caracteres")
	private String bloque;

	/** Salón. */
	@NotBlank(message = "El salón es obligatorio")
	@Size(max = 20, message = "No puede superar 20 caracteres")
	private String salon;

	/** Franjas semanales; al menos una. */
	@NotEmpty(message = "Debe tener al menos un horario")
	@Size(max = 14, message = "No puede tener más de 14 horarios")
	@Valid
	private List<HorarioDto> horarios;

	/** Constructor sin argumentos que exige Jackson. */
	public GrupoRequest() {
	}

	/** @return asignatura que se ofrece */
	public Long getAsignaturaId() {
		return asignaturaId;
	}

	/** @param asignaturaId asignatura que se ofrece */
	public void setAsignaturaId(Long asignaturaId) {
		this.asignaturaId = asignaturaId;
	}

	/** @return periodo de la oferta */
	public Long getPeriodoId() {
		return periodoId;
	}

	/** @param periodoId periodo de la oferta */
	public void setPeriodoId(Long periodoId) {
		this.periodoId = periodoId;
	}

	/** @return profesor responsable */
	public Long getProfesorId() {
		return profesorId;
	}

	/** @param profesorId profesor responsable */
	public void setProfesorId(Long profesorId) {
		this.profesorId = profesorId;
	}

	/** @return bloque del edificio */
	public String getBloque() {
		return bloque;
	}

	/** @param bloque bloque del edificio */
	public void setBloque(String bloque) {
		this.bloque = bloque;
	}

	/** @return salón */
	public String getSalon() {
		return salon;
	}

	/** @param salon salón */
	public void setSalon(String salon) {
		this.salon = salon;
	}

	/** @return franjas semanales */
	public List<HorarioDto> getHorarios() {
		return horarios;
	}

	/** @param horarios franjas semanales */
	public void setHorarios(List<HorarioDto> horarios) {
		this.horarios = horarios == null ? null : new ArrayList<>(horarios);
	}
}
