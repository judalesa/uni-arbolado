package co.edu.unbosque.arbolado.dto;

/** Datos mínimos de una persona, para mostrarla dentro de otro recurso. */
public class UsuarioResumenDto {

	/** Identificador. */
	private Long id;

	/** Documento de 10 dígitos. */
	private String documento;

	/** Nombre completo. */
	private String nombreCompleto;

	/** Constructor sin argumentos que exige ModelMapper. */
	public UsuarioResumenDto() {
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @param id identificador */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return documento */
	public String getDocumento() {
		return documento;
	}

	/** @param documento documento */
	public void setDocumento(String documento) {
		this.documento = documento;
	}

	/** @return nombre completo */
	public String getNombreCompleto() {
		return nombreCompleto;
	}

	/** @param nombreCompleto nombre completo */
	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}
}
