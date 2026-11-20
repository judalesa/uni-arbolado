package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.model.EstadoInscripcion;

import java.math.BigDecimal;
import java.time.Instant;

/** Inscripción de un estudiante con su grupo, su estado y sus notas. */
public class InscripcionDto {

	/** Identificador. */
	private Long id;

	/** Estudiante. */
	private UsuarioResumenDto estudiante;

	/** Grupo con asignatura, horarios y cupos. */
	private GrupoDto grupo;

	/** Estado. */
	private EstadoInscripcion estado;

	/** Nota del corte 1, o {@code null}. */
	private BigDecimal notaCorte1;

	/** Nota del corte 2, o {@code null}. */
	private BigDecimal notaCorte2;

	/** Nota del corte 3, o {@code null}. */
	private BigDecimal notaCorte3;

	/** Nota definitiva ya redondeada, o {@code null}. */
	private BigDecimal notaDefinitiva;

	/** Instante de la inscripción (UTC). */
	private Instant fechaInscripcion;

	/** Constructor sin argumentos. */
	public InscripcionDto() {
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @param id identificador */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return estudiante */
	public UsuarioResumenDto getEstudiante() {
		return estudiante;
	}

	/** @param estudiante estudiante */
	public void setEstudiante(UsuarioResumenDto estudiante) {
		this.estudiante = estudiante;
	}

	/** @return grupo */
	public GrupoDto getGrupo() {
		return grupo;
	}

	/** @param grupo grupo */
	public void setGrupo(GrupoDto grupo) {
		this.grupo = grupo;
	}

	/** @return estado */
	public EstadoInscripcion getEstado() {
		return estado;
	}

	/** @param estado estado */
	public void setEstado(EstadoInscripcion estado) {
		this.estado = estado;
	}

	/** @return nota del corte 1 */
	public BigDecimal getNotaCorte1() {
		return notaCorte1;
	}

	/** @param notaCorte1 nota del corte 1 */
	public void setNotaCorte1(BigDecimal notaCorte1) {
		this.notaCorte1 = notaCorte1;
	}

	/** @return nota del corte 2 */
	public BigDecimal getNotaCorte2() {
		return notaCorte2;
	}

	/** @param notaCorte2 nota del corte 2 */
	public void setNotaCorte2(BigDecimal notaCorte2) {
		this.notaCorte2 = notaCorte2;
	}

	/** @return nota del corte 3 */
	public BigDecimal getNotaCorte3() {
		return notaCorte3;
	}

	/** @param notaCorte3 nota del corte 3 */
	public void setNotaCorte3(BigDecimal notaCorte3) {
		this.notaCorte3 = notaCorte3;
	}

	/** @return nota definitiva */
	public BigDecimal getNotaDefinitiva() {
		return notaDefinitiva;
	}

	/** @param notaDefinitiva nota definitiva */
	public void setNotaDefinitiva(BigDecimal notaDefinitiva) {
		this.notaDefinitiva = notaDefinitiva;
	}

	/** @return instante de la inscripción */
	public Instant getFechaInscripcion() {
		return fechaInscripcion;
	}

	/** @param fechaInscripcion instante de la inscripción */
	public void setFechaInscripcion(Instant fechaInscripcion) {
		this.fechaInscripcion = fechaInscripcion;
	}
}
