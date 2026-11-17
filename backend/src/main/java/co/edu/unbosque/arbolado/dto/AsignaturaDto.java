package co.edu.unbosque.arbolado.dto;

import java.util.ArrayList;
import java.util.List;

/** Asignatura del plan de estudios con sus prerrequisitos (esquema {@code AsignaturaDto} del contrato). */
public class AsignaturaDto {

	/** Identificador. */
	private Long id;

	/** Código, por ejemplo {@code MAT101}. */
	private String codigo;

	/** Nombre. */
	private String nombre;

	/** Descripción breve. */
	private String descripcion;

	/** Semestre del plan (1 a 9). */
	private int semestre;

	/** Créditos (1 a 5). */
	private int creditos;

	/** Asignaturas que deben estar aprobadas antes de cursar esta. */
	private List<AsignaturaResumenDto> prerrequisitos = new ArrayList<>();

	/** Constructor sin argumentos que exige ModelMapper. */
	public AsignaturaDto() {
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

	/** @return descripción */
	public String getDescripcion() {
		return descripcion;
	}

	/** @param descripcion descripción */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	/** @return semestre */
	public int getSemestre() {
		return semestre;
	}

	/** @param semestre semestre */
	public void setSemestre(int semestre) {
		this.semestre = semestre;
	}

	/** @return créditos */
	public int getCreditos() {
		return creditos;
	}

	/** @param creditos créditos */
	public void setCreditos(int creditos) {
		this.creditos = creditos;
	}

	/** @return prerrequisitos de la asignatura */
	public List<AsignaturaResumenDto> getPrerrequisitos() {
		return prerrequisitos;
	}

	/** @param prerrequisitos prerrequisitos de la asignatura */
	public void setPrerrequisitos(List<AsignaturaResumenDto> prerrequisitos) {
		this.prerrequisitos = prerrequisitos == null ? null : new java.util.ArrayList<>(prerrequisitos);
	}
}
