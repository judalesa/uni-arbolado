package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;

import java.time.Instant;

/** Datos públicos de una cuenta; nunca incluye la contraseña. */
public class UsuarioDto {

	/** Identificador. */
	private Long id;

	/** Documento de 10 dígitos. */
	private String documento;

	/** Nombre completo. */
	private String nombreCompleto;

	/** Correo electrónico. */
	private String correo;

	/** Rol de la cuenta. */
	private Rol rol;

	/** Estado de la cuenta. */
	private EstadoCuenta estado;

	/** Instante de creación (UTC). */
	private Instant fechaCreacion;

	/** Constructor sin argumentos que exige ModelMapper. */
	public UsuarioDto() {
	}

	/** @return identificador */
	public Long getId() {
		return id;
	}

	/** @param id identificador */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return documento */
	public String getDocumento() {
		return documento;
	}

	/** @param documento documento */
	public void setDocumento(String documento) {
		this.documento = documento;
	}

	/** @return nombre completo */
	public String getNombreCompleto() {
		return nombreCompleto;
	}

	/** @param nombreCompleto nombre completo */
	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}

	/** @return correo */
	public String getCorreo() {
		return correo;
	}

	/** @param correo correo */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/** @return rol */
	public Rol getRol() {
		return rol;
	}

	/** @param rol rol */
	public void setRol(Rol rol) {
		this.rol = rol;
	}

	/** @return estado */
	public EstadoCuenta getEstado() {
		return estado;
	}

	/** @param estado estado */
	public void setEstado(EstadoCuenta estado) {
		this.estado = estado;
	}

	/** @return instante de creación */
	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	/** @param fechaCreacion instante de creación */
	public void setFechaCreacion(Instant fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}
}
