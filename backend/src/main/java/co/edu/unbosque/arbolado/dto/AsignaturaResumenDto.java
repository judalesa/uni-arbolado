package co.edu.unbosque.arbolado.dto;

/** Datos mínimos de una asignatura, usados para listar prerrequisitos. */
public class AsignaturaResumenDto {

	/** Identificador. */
	private Long id;

	/** Código, por ejemplo {@code DSI301}. */
	private String codigo;

	/** Nombre. */
	private String nombre;

	/** Créditos. */
	private int creditos;

	/** Constructor sin argumentos que exige ModelMapper. */
	public AsignaturaResumenDto() {
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @param id identificador */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return código */
	public String getCodigo() {
		return codigo;
	}

	/** @param codigo código */
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	/** @return nombre */
	public String getNombre() {
		return nombre;
	}

	/** @param nombre nombre */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/** @return créditos */
	public int getCreditos() {
		return creditos;
	}

	/** @param creditos créditos */
	public void setCreditos(int creditos) {
		this.creditos = creditos;
	}
}
