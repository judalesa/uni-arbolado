package co.edu.unbosque.arbolado.dto;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de {@code POST /api/inscripciones}: el grupo en el que se quiere inscribir el estudiante. */
public class InscripcionRequest {

	/** Grupo elegido. */
	@NotNull(message = "El grupo es obligatorio")
	private Long grupoId;

	/** Constructor sin argumentos que exige Jackson. */
	public InscripcionRequest() {
	}

	/** @return grupo elegido */
	public Long getGrupoId() {
		return grupoId;
	}

	/** @param grupoId grupo elegido */
	public void setGrupoId(Long grupoId) {
		this.grupoId = grupoId;
	}
}
