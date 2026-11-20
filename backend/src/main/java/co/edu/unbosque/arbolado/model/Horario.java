package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

import java.time.LocalTime;

/** Franja semanal de un grupo: día, hora de inicio y hora de fin. */
@Entity
@Table(name = "horario")
@Check(constraints = "hora_inicio < hora_fin")
public class Horario {

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Grupo al que pertenece la franja. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grupo_id", nullable = false, updatable = false)
	private Grupo grupo;

	/** Día de la semana. */
	@Enumerated(EnumType.STRING)
	@Column(name = "dia_semana", nullable = false, length = 10)
	private DiaSemana diaSemana;

	/** Hora de inicio. */
	@Column(name = "hora_inicio", nullable = false)
	private LocalTime horaInicio;

	/** Hora de fin; siempre posterior a la de inicio. */
	@Column(name = "hora_fin", nullable = false)
	private LocalTime horaFin;

	/** Constructor sin argumentos que exige JPA. */
	protected Horario() {
	}

	/**
	 * Crea una franja. El identificador lo asigna la base de datos.
	 *
	 * @param grupo      grupo al que pertenece
	 * @param diaSemana  día de la semana
	 * @param horaInicio hora de inicio
	 * @param horaFin    hora de fin
	 */
	public Horario(Grupo grupo, DiaSemana diaSemana, LocalTime horaInicio, LocalTime horaFin) {
		this.grupo = grupo;
		this.diaSemana = diaSemana;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @return día de la semana */
	public DiaSemana getDiaSemana() {
		return diaSemana;
	}

	/** @return hora de inicio */
	public LocalTime getHoraInicio() {
		return horaInicio;
	}

	/** @return hora de fin */
	public LocalTime getHoraFin() {
		return horaFin;
	}
}
