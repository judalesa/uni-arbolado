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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un intento de un estudiante en un grupo. El historial conserva todos los intentos (RN-20). Las dos
 * restricciones únicas son la red de seguridad de la base: nadie se inscribe dos veces en el mismo grupo y
 * nadie tiene dos inscripciones vigentes de la misma asignatura (RN-19), aun con peticiones simultáneas.
 */
@Entity
@Table(name = "inscripcion", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"estudiante_id", "grupo_id"}),
		@UniqueConstraint(columnNames = {"estudiante_id", "asignatura_id", "marca_vigente"})})
@Check(constraints = "(nota_corte1 is null or nota_corte1 between 0 and 5) and (nota_corte2 is null or nota_corte2 between 0 and 5) "
		+ "and (nota_corte3 is null or nota_corte3 between 0 and 5) and (nota_definitiva is null or nota_definitiva between 0 and 5)")
public class Inscripcion {

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Estudiante que cursa. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estudiante_id", nullable = false, updatable = false)
	private Usuario estudiante;

	/** Grupo en el que se inscribió. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "grupo_id", nullable = false, updatable = false)
	private Grupo grupo;

	/** Periodo del grupo, copiado al crear para consultar y validar sin uniones. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "periodo_id", nullable = false, updatable = false)
	private Periodo periodo;

	/** Asignatura del grupo, copiada al crear por la misma razón. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "asignatura_id", nullable = false, updatable = false)
	private Asignatura asignatura;

	/** Estado actual. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EstadoInscripcion estado = EstadoInscripcion.CURSANDO;

	/** Nota del primer corte (30 %); se escribe una sola vez (D-20). */
	@Column(name = "nota_corte1", precision = 2, scale = 1)
	private BigDecimal notaCorte1;

	/** Nota del segundo corte (30 %). */
	@Column(name = "nota_corte2", precision = 2, scale = 1)
	private BigDecimal notaCorte2;

	/** Nota del tercer corte (40 %). */
	@Column(name = "nota_corte3", precision = 2, scale = 1)
	private BigDecimal notaCorte3;

	/** Nota definitiva ya redondeada; nula hasta que hay tres cortes. */
	@Column(name = "nota_definitiva", precision = 2, scale = 1)
	private BigDecimal notaDefinitiva;

	/**
	 * Verdadero mientras la inscripción esté cursando o aprobada y nulo si está reprobada. Como la
	 * restricción única ignora los nulos, una reprobada no impide reinscribir la asignatura (RN-20).
	 */
	@Column(name = "marca_vigente")
	private Boolean marcaVigente = Boolean.TRUE;

	/** Instante de la inscripción, en UTC. */
	@Column(name = "fecha_inscripcion", nullable = false, updatable = false)
	private Instant fechaInscripcion;

	/** Constructor sin argumentos que exige JPA. */
	protected Inscripcion() {
	}

	/**
	 * Crea una inscripción cursando. El periodo y la asignatura se toman del grupo.
	 *
	 * @param estudiante estudiante que se inscribe
	 * @param grupo      grupo elegido
	 */
	public Inscripcion(Usuario estudiante, Grupo grupo) {
		this.estudiante = estudiante;
		this.grupo = grupo;
		this.periodo = grupo.getPeriodo();
		this.asignatura = grupo.getAsignatura();
		this.fechaInscripcion = Instant.now();
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @return estudiante */
	public Usuario getEstudiante() {
		return estudiante;
	}

	/** @return grupo */
	public Grupo getGrupo() {
		return grupo;
	}

	/** @return periodo */
	public Periodo getPeriodo() {
		return periodo;
	}

	/** @return asignatura */
	public Asignatura getAsignatura() {
		return asignatura;
	}

	/** @return estado */
	public EstadoInscripcion getEstado() {
		return estado;
	}

	/** @return nota del primer corte, o {@code null} */
	public BigDecimal getNotaCorte1() {
		return notaCorte1;
	}

	/** @return nota del segundo corte, o {@code null} */
	public BigDecimal getNotaCorte2() {
		return notaCorte2;
	}

	/** @return nota del tercer corte, o {@code null} */
	public BigDecimal getNotaCorte3() {
		return notaCorte3;
	}

	/** @return nota definitiva, o {@code null} */
	public BigDecimal getNotaDefinitiva() {
		return notaDefinitiva;
	}

	/** @return instante de la inscripción */
	public Instant getFechaInscripcion() {
		return fechaInscripcion;
	}
}
