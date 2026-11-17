package co.edu.unbosque.arbolado.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Identificador de un JWT cerrado con el logout; el token deja de ser válido aunque no haya vencido. */
@Entity
@Table(name = "tokenrevocado")
public class TokenRevocado {

	/** Identificador generado por la base de datos. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Identificador único del token ({@code jti}). */
	@Column(nullable = false, unique = true, length = 36, updatable = false)
	private String jti;

	/** Instante en que el token vence; después de esto el registro se puede purgar. */
	@Column(name = "expira_en", nullable = false, updatable = false)
	private Instant expiraEn;

	/** Constructor sin argumentos que exige JPA. */
	protected TokenRevocado() {
	}

	/**
	 * Crea el registro. El identificador lo asigna la base de datos.
	 *
	 * @param jti      identificador del token
	 * @param expiraEn instante de vencimiento del token
	 */
	public TokenRevocado(String jti, Instant expiraEn) {
		this.jti = jti;
		this.expiraEn = expiraEn;
	}

	/** @return identificador del registro */
	public Long getId() {
		return id;
	}

	/** @return identificador del token */
	public String getJti() {
		return jti;
	}

	/** @return instante de vencimiento del token */
	public Instant getExpiraEn() {
		return expiraEn;
	}
}
