package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Código de verificación de correo enviado a un usuario; solo se guarda su huella HMAC. */
@Entity
@Table(name = "codigoverificacion")
public class CodigoVerificacion {

	/** Intentos fallidos a partir de los cuales el código deja de servir. */
	public static final int MAX_INTENTOS = 5;

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // skipcq: JAVA-E1065

	/** Cuenta a la que se envió el código. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, updatable = false)
	private Usuario usuario;

	/** HMAC-SHA256 del código en hexadecimal; el código nunca se guarda en claro. */
	@Column(name = "codigo_hmac", nullable = false, length = 64, updatable = false)
	private String codigoHmac;

	/** Instante de creación en UTC. */
	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private Instant fechaCreacion;

	/** Instante en que el código vence, en UTC. */
	@Column(name = "fecha_expiracion", nullable = false, updatable = false)
	private Instant fechaExpiracion;

	/** Intentos fallidos acumulados. */
	@Column(name = "intentos_fallidos", nullable = false)
	private int intentosFallidos;

	/** Verdadero cuando el código ya se usó con éxito. */
	@Column(nullable = false)
	private boolean usado;

	/** Verdadero cuando un código más reciente lo reemplazó. */
	@Column(nullable = false)
	private boolean invalidado;

	/** Constructor sin argumentos que exige JPA. */
	protected CodigoVerificacion() {
	}

	/**
	 * Crea un código nuevo. El identificador lo asigna la base de datos.
	 *
	 * @param usuario         cuenta destinataria
	 * @param codigoHmac      huella HMAC del código
	 * @param fechaCreacion   instante de creación
	 * @param fechaExpiracion instante de vencimiento
	 */
	public CodigoVerificacion(Usuario usuario, String codigoHmac, Instant fechaCreacion, Instant fechaExpiracion) {
		this.usuario = usuario;
		this.codigoHmac = codigoHmac;
		this.fechaCreacion = fechaCreacion;
		this.fechaExpiracion = fechaExpiracion;
	}

	/**
	 * Indica si el código aún se puede usar: sin usar, sin reemplazar, sin vencer y con intentos disponibles.
	 *
	 * @param ahora instante de la comprobación
	 * @return {@code true} si es utilizable
	 */
	public boolean estaVigente(Instant ahora) {
		return !usado && !invalidado && intentosFallidos < MAX_INTENTOS && ahora.isBefore(fechaExpiracion);
	}

	/** Cuenta un intento fallido. */
	public void registrarFallo() {
		this.intentosFallidos++;
	}

	/** Marca el código como usado. */
	public void marcarUsado() {
		this.usado = true;
	}

	/** Marca el código como reemplazado por uno más reciente. */
	public void invalidar() {
		this.invalidado = true;
	}

	/** @return identificador del registro */
	public Long getId() {
		return id;
	}

	/** @return cuenta destinataria */
	public Usuario getUsuario() {
		return usuario;
	}

	/** @return huella HMAC del código */
	public String getCodigoHmac() {
		return codigoHmac;
	}

	/** @return instante de creación (UTC) */
	public Instant getFechaCreacion() {
		return fechaCreacion;
	}

	/** @return instante de vencimiento (UTC) */
	public Instant getFechaExpiracion() {
		return fechaExpiracion;
	}

	/** @return intentos fallidos acumulados */
	public int getIntentosFallidos() {
		return intentosFallidos;
	}

	/** @return {@code true} si ya se usó */
	public boolean isUsado() {
		return usado;
	}

	/** @return {@code true} si fue reemplazado */
	public boolean isInvalidado() {
		return invalidado;
	}

	/**
	 * Representación para registros: omite la huella a propósito.
	 *
	 * @return texto con identificador, intentos y estado
	 */
	@Override
	public String toString() {
		return "CodigoVerificacion{id=" + id + ", intentos=" + intentosFallidos + ", usado=" + usado
				+ ", invalidado=" + invalidado + "}";
	}
}
