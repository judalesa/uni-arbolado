package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.ArrayList;
import java.util.List;

/** Asignatura del plan de estudios. Se precarga y nunca se modifica desde la API. */
@Entity
@Table(name = "asignatura")
public class Asignatura {

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Código único, por ejemplo {@code MAT101}. */
	@Column(nullable = false, unique = true, length = 10)
	private String codigo;

	/** Nombre único de la asignatura. */
	@Column(nullable = false, unique = true, length = 120)
	private String nombre;

	/** Descripción breve. */
	@Column(nullable = false, length = 500)
	private String descripcion;

	/** Semestre del plan (1 a 9). */
	@Min(1)
	@Max(9)
	@Column(nullable = false)
	private int semestre;

	/** Créditos (1 a 5). */
	@Min(1)
	@Max(5)
	@Column(nullable = false)
	private int creditos;

	/** Relaciones de prerrequisito de esta asignatura. */
	@OneToMany(mappedBy = "asignatura")
	private List<Prerrequisito> prerrequisitos = new ArrayList<>();

	/** Constructor sin argumentos que exige JPA. */
	protected Asignatura() {
	}

	/**
	 * Crea una asignatura. El identificador lo asigna la base de datos.
	 *
	 * @param codigo      código único
	 * @param nombre      nombre único
	 * @param descripcion descripción breve
	 * @param semestre    semestre del plan (1 a 9)
	 * @param creditos    créditos (1 a 5)
	 */
	public Asignatura(String codigo, String nombre, String descripcion, int semestre, int creditos) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.semestre = semestre;
		this.creditos = creditos;
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @return código de la asignatura */
	public String getCodigo() {
		return codigo;
	}

	/** @return nombre de la asignatura */
	public String getNombre() {
		return nombre;
	}

	/** @return descripción breve */
	public String getDescripcion() {
		return descripcion;
	}

	/** @return semestre del plan */
	public int getSemestre() {
		return semestre;
	}

	/** @return créditos */
	public int getCreditos() {
		return creditos;
	}

	/** @return relaciones de prerrequisito de esta asignatura */
	public List<Prerrequisito> getPrerrequisitos() {
		return prerrequisitos;
	}
}
