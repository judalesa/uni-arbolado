package co.edu.unbosque.arbolado.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

/** Oferta de una asignatura en un periodo, con profesor, bloque, salón, horarios y cupo de 30. */
@Entity
@Table(name = "grupo", uniqueConstraints = @UniqueConstraint(columnNames = {"asignatura_id", "periodo_id", "numero"}))
public class Grupo {

	/** Cupo máximo de todos los grupos; no se puede cambiar desde la API. */
	public static final int CUPO_MAXIMO = 30;

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Número del grupo dentro de la asignatura y el periodo (1, 2, 3…). */
	@Column(nullable = false)
	private int numero;

	/** Asignatura que se ofrece. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "asignatura_id", nullable = false)
	private Asignatura asignatura;

	/** Periodo en que se ofrece. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "periodo_id", nullable = false)
	private Periodo periodo;

	/** Profesor responsable; su rol debe ser PROFESOR. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profesor_id", nullable = false)
	private Usuario profesor;

	/** Bloque del edificio. */
	@Column(nullable = false, length = 20)
	private String bloque;

	/** Salón. */
	@Column(nullable = false, length = 20)
	private String salon;

	/** Cupo máximo; vale siempre {@link #CUPO_MAXIMO}. */
	@Column(name = "cupo_maximo", nullable = false, updatable = false)
	private int cupoMaximo = CUPO_MAXIMO;

	/** Franjas semanales del grupo. */
	@OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Horario> horarios = new ArrayList<>();

	/** Constructor sin argumentos que exige JPA. */
	protected Grupo() {
	}

	/**
	 * Crea un grupo sin horarios. El identificador lo asigna la base de datos.
	 *
	 * @param numero     número dentro de la asignatura y el periodo
	 * @param asignatura asignatura ofrecida
	 * @param periodo    periodo de la oferta
	 * @param profesor   profesor responsable
	 * @param bloque     bloque del edificio
	 * @param salon      salón
	 */
	public Grupo(int numero, Asignatura asignatura, Periodo periodo, Usuario profesor, String bloque, String salon) {
		this.numero = numero;
		this.asignatura = asignatura;
		this.periodo = periodo;
		this.profesor = profesor;
		this.bloque = bloque;
		this.salon = salon;
	}

	/**
	 * Cambia los datos que siempre se pueden editar, haya o no inscritos.
	 *
	 * @param profesor profesor responsable
	 * @param bloque   bloque del edificio
	 * @param salon    salón
	 */
	public void actualizarUbicacion(Usuario profesor, String bloque, String salon) {
		this.profesor = profesor;
		this.bloque = bloque;
		this.salon = salon;
	}

	/**
	 * Cambia la asignatura, el periodo y el número; solo se usa si el grupo no tiene inscritos.
	 *
	 * @param asignatura nueva asignatura
	 * @param periodo    nuevo periodo
	 * @param numero     número dentro de la nueva asignatura y periodo
	 */
	public void reubicar(Asignatura asignatura, Periodo periodo, int numero) {
		this.asignatura = asignatura;
		this.periodo = periodo;
		this.numero = numero;
	}

	/**
	 * Reemplaza todas las franjas; solo se usa si el grupo no tiene inscritos.
	 *
	 * @param nuevos horarios nuevos, ya validados
	 */
	public void reemplazarHorarios(List<Horario> nuevos) {
		horarios.clear();
		horarios.addAll(nuevos);
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @return número dentro de la asignatura y el periodo */
	public int getNumero() {
		return numero;
	}

	/** @return asignatura ofrecida */
	public Asignatura getAsignatura() {
		return asignatura;
	}

	/** @return periodo de la oferta */
	public Periodo getPeriodo() {
		return periodo;
	}

	/** @return profesor responsable */
	public Usuario getProfesor() {
		return profesor;
	}

	/** @return bloque del edificio */
	public String getBloque() {
		return bloque;
	}

	/** @return salón */
	public String getSalon() {
		return salon;
	}

	/** @return cupo máximo */
	public int getCupoMaximo() {
		return cupoMaximo;
	}

	/** @return copia de solo lectura de las franjas */
	public List<Horario> getHorarios() {
		return List.copyOf(horarios);
	}

	/**
	 * Representación para registros.
	 *
	 * @return texto con identificador y número
	 */
	@Override
	public String toString() {
		return "Grupo{id=" + id + ", numero=" + numero + "}";
	}
}
