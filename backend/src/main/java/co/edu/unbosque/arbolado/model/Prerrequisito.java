package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Relación "para cursar la asignatura hay que aprobar el requisito". */
@Entity
@Table(name = "prerrequisito", uniqueConstraints = @UniqueConstraint(columnNames = {"asignatura_id", "requisito_id"}))
public class Prerrequisito {

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Asignatura que exige el requisito. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "asignatura_id", nullable = false)
	private Asignatura asignatura;

	/** Asignatura que debe estar aprobada. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requisito_id", nullable = false)
	private Asignatura requisito;

	/** Constructor sin argumentos que exige JPA. */
	protected Prerrequisito() {
	}

	/**
	 * Crea la relación.
	 *
	 * @param asignatura asignatura que exige el requisito
	 * @param requisito  asignatura que debe estar aprobada
	 * @throws IllegalArgumentException si una asignatura se pide a sí misma como requisito
	 */
	public Prerrequisito(Asignatura asignatura, Asignatura requisito) {
		if (asignatura.getCodigo().equals(requisito.getCodigo())) {
			throw new IllegalArgumentException("Una asignatura no puede ser su propio prerrequisito");
		}
		this.asignatura = asignatura;
		this.requisito = requisito;
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @return asignatura que exige el requisito */
	public Asignatura getAsignatura() {
		return asignatura;
	}

	/** @return asignatura requisito */
	public Asignatura getRequisito() {
		return requisito;
	}
}
