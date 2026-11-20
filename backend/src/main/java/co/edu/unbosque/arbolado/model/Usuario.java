package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;

/** Cuenta de una persona en el sistema (estudiante, profesor o administrador). */
@Entity
@Table(name = "usuario")
public class Usuario {

	/** Identificador generado por la base de datos; no se asigna desde el código. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Documento de 10 dígitos; se guarda como texto porque puede empezar en cero. */
	@Column(nullable = false, unique = true, length = 10)
	private String documento;

	/** Nombre completo. */
	@Column(name = "nombre_completo", nullable = false, length = 120)
	private String nombreCompleto;

	/** Correo electrónico en minúsculas. */
	@Column(nullable = false, unique = true, length = 120)
	private String correo;

	/** Contraseña ya codificada; nunca en texto plano. */
	@Column(nullable = false, length = 255)
	private String contrasena;

	/** Rol de la cuenta. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	/** Estado de la cuenta. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoCuenta estado;

	/** Instante de creación en UTC. */
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	/** Constructor sin argumentos que exige JPA. */
	protected Usuario() {
	}

	/**
	 * Crea una cuenta. El identificador lo asigna la base de datos.
	 *
	 * @param documento      documento de 10 dígitos
	 * @param nombreCompleto nombre completo
	 * @param correo         correo electrónico (se normaliza a minúsculas)
	 * @param contrasena     contraseña ya codificada
	 * @param rol            rol de la cuenta
	 * @param estado         estado inicial
	 */
	public Usuario(String documento, String nombreCompleto, String correo, String contrasena,
				   Rol rol, EstadoCuenta estado) {
		this.documento = documento;
		this.nombreCompleto = nombreCompleto;
		this.correo = correo.trim().toLowerCase(Locale.ROOT);
		this.contrasena = contrasena;
		this.rol = rol;
		this.estado = estado;
		this.fechaCreacion = Instant.now();
	}

	/** @return identificador de la cuenta */
	public Long getId() {
		return id;
	}

	/** @return documento de 10 dígitos */
	public String getDocumento() {
		return documento;
	}

	/** @return nombre completo */
	public String getNombreCompleto() {
		return nombreCompleto;
	}

	/** @return correo en minúsculas */
	public String getCorreo() {
		return correo;
	}

	/** @return contraseña codificada */
	public String getContrasena() {
		return contrasena;
	}

	/** @return rol de la cuenta */
	public Rol getRol() {
		return rol;
	}

	/** @return estado de la cuenta */
	public EstadoCuenta getEstado() {
		return estado;
	}

	/** @return instante de creación (UTC) */
	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	/** Marca la cuenta como verificada. */
	public void activar() {
		this.estado = EstadoCuenta.ACTIVO;
	}

	/**
	 * Reemplaza los datos de una cuenta pendiente que se vuelve a registrar.
	 *
	 * @param documento      documento de 10 dígitos
	 * @param nombreCompleto nombre completo
	 * @param correo         correo electrónico (se normaliza a minúsculas)
	 * @param contrasena     contraseña ya codificada
	 */
	public void actualizarRegistro(String documento, String nombreCompleto, String correo, String contrasena) {
		this.documento = documento;
		this.nombreCompleto = nombreCompleto;
		this.correo = correo.trim().toLowerCase(Locale.ROOT);
		this.contrasena = contrasena;
	}

	/**
	 * Representación para registros: omite la contraseña a propósito.
	 *
	 * @return texto con identificador, correo, rol y estado
	 */
	@Override
	public String toString() {
		return "Usuario{id=" + id + ", correo=" + correo + ", rol=" + rol + ", estado=" + estado + "}";
	}
}
