package co.edu.unbosque.arbolado.dto;

import java.util.List;

/** Grupo con su asignatura, periodo, profesor, horarios y cupos. */
public class GrupoDto {

	/** Identificador. */
	private Long id;

	/** Número dentro de la asignatura y el periodo. */
	private int numero;

	/** Asignatura ofrecida. */
	private AsignaturaResumenDto asignatura;

	/** Periodo de la oferta. */
	private PeriodoDto periodo;

	/** Profesor responsable. */
	private UsuarioResumenDto profesor;

	/** Bloque del edificio. */
	private String bloque;

	/** Salón. */
	private String salon;

	/** Cupo máximo. */
	private int cupoMaximo;

	/** Estudiantes en estado CURSANDO. */
	private long inscritos;

	/** Cupos que quedan. */
	private long cuposDisponibles;

	/** Franjas semanales, ordenadas por día y hora. */
	private List<HorarioDto> horarios;

	/** Constructor sin argumentos. */
	public GrupoDto() {
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @param id identificador */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return número dentro de la asignatura y el periodo */
	public int getNumero() {
		return numero;
	}

	/** @param numero número dentro de la asignatura y el periodo */
	public void setNumero(int numero) {
		this.numero = numero;
	}

	/** @return asignatura ofrecida */
	public AsignaturaResumenDto getAsignatura() {
		return asignatura;
	}

	/** @param asignatura asignatura ofrecida */
	public void setAsignatura(AsignaturaResumenDto asignatura) {
		this.asignatura = asignatura;
	}

	/** @return periodo de la oferta */
	public PeriodoDto getPeriodo() {
		return periodo;
	}

	/** @param periodo periodo de la oferta */
	public void setPeriodo(PeriodoDto periodo) {
		this.periodo = periodo;
	}

	/** @return profesor responsable */
	public UsuarioResumenDto getProfesor() {
		return profesor;
	}

	/** @param profesor profesor responsable */
	public void setProfesor(UsuarioResumenDto profesor) {
		this.profesor = profesor;
	}

	/** @return bloque del edificio */
	public String getBloque() {
		return bloque;
	}

	/** @param bloque bloque del edificio */
	public void setBloque(String bloque) {
		this.bloque = bloque;
	}

	/** @return salón */
	public String getSalon() {
		return salon;
	}

	/** @param salon salón */
	public void setSalon(String salon) {
		this.salon = salon;
	}

	/** @return cupo máximo */
	public int getCupoMaximo() {
		return cupoMaximo;
	}

	/** @param cupoMaximo cupo máximo */
	public void setCupoMaximo(int cupoMaximo) {
		this.cupoMaximo = cupoMaximo;
	}

	/** @return estudiantes cursando */
	public long getInscritos() {
		return inscritos;
	}

	/** @param inscritos estudiantes cursando */
	public void setInscritos(long inscritos) {
		this.inscritos = inscritos;
	}

	/** @return cupos que quedan */
	public long getCuposDisponibles() {
		return cuposDisponibles;
	}

	/** @param cuposDisponibles cupos que quedan */
	public void setCuposDisponibles(long cuposDisponibles) {
		this.cuposDisponibles = cuposDisponibles;
	}

	/** @return franjas semanales */
	public List<HorarioDto> getHorarios() {
		return horarios;
	}

	/** @param horarios franjas semanales */
	public void setHorarios(List<HorarioDto> horarios) {
		this.horarios = horarios;
	}
}
