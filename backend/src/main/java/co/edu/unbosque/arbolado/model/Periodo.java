package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Periodo académico con código único, por ejemplo 2026-2 (RN-13). Solo uno puede estar activo (RN-14). */
@Entity
@Table(name = "periodo")
public class Periodo {

	/** Identificador generado por la base de datos; no se asigna desde el código. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Código único con formato AAAA-N. */
	@Column(nullable = false, unique = true, length = 6)
	private String codigo;

	/** Fecha de inicio (informativa). */
	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	/** Fecha de fin (informativa). */
	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;

	/** Verdadero si es el periodo vigente; es lo que ve la API. */
	@Column(nullable = false)
	private boolean activo;

	/**
	 * {@code TRUE} en el periodo activo y {@code NULL} en los demás. El índice único admite varios
	 * {@code NULL}, así que la base de datos misma impide dos periodos activos.
	 */
	@Column(name = "marca_activo", unique = true)
	private Boolean marcaActivo;

	/** Constructor sin argumentos que exige JPA. */
	protected Periodo() {
	}

	/**
	 * Crea un periodo inactivo. El identificador lo asigna la base de datos.
	 *
	 * @param codigo      código con formato AAAA-N
	 * @param fechaInicio fecha de inicio
	 * @param fechaFin    fecha de fin
	 */
	public Periodo(String codigo, LocalDate fechaInicio, LocalDate fechaFin) {
		this.codigo = codigo;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
	}

	/** @return identificador del periodo */
	public Long getId() {
		return id;
	}

	/** @return código del periodo */
	public String getCodigo() {
		return codigo;
	}

	/** @return fecha de inicio */
	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	/** @return fecha de fin */
	public LocalDate getFechaFin() {
		return fechaFin;
	}

	/** @return {@code true} si es el periodo vigente */
	public boolean isActivo() {
		return activo;
	}

	/**
	 * Cambia los datos del periodo sin tocar su estado activo.
	 *
	 * @param codigo      código con formato AAAA-N
	 * @param fechaInicio fecha de inicio
	 * @param fechaFin    fecha de fin
	 */
	public void actualizar(String codigo, LocalDate fechaInicio, LocalDate fechaFin) {
		this.codigo = codigo;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
	}

	/** Marca el periodo como el vigente. */
	public void activar() {
		this.activo = true;
		this.marcaActivo = Boolean.TRUE;
	}

	/** Quita la marca de vigente. */
	public void desactivar() {
		this.activo = false;
		this.marcaActivo = null;
	}

	/**
	 * Representación para registros.
	 *
	 * @return texto con identificador, código y estado
	 */
	@Override
	public String toString() {
		return "Periodo{id=" + id + ", codigo=" + codigo + ", activo=" + activo + "}";
	}
}
