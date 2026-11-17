package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.TokenRevocado;
import co.edu.unbosque.arbolado.repository.TokenRevocadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Lista de tokens cerrados con el logout (RF-08). */
@Service
public class TokenRevocadoService {

	/** Repositorio de tokens revocados. */
	private final TokenRevocadoRepository repositorio;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio repositorio de tokens revocados
	 */
	public TokenRevocadoService(TokenRevocadoRepository repositorio) {
		this.repositorio = repositorio;
	}

	/**
	 * Revoca un token hasta su vencimiento. De paso borra los registros de tokens ya vencidos, para que la
	 * tabla no crezca sin límite y sin necesidad de una tarea programada.
	 *
	 * @param jti      identificador del token
	 * @param expiraEn instante en que el token vence
	 */
	@Transactional
	public void revocar(String jti, Instant expiraEn) {
		repositorio.purgarVencidos(Instant.now());
		if (!repositorio.existsByJti(jti)) {
			repositorio.save(new TokenRevocado(jti, expiraEn));
		}
	}

	/**
	 * Indica si un token fue revocado.
	 *
	 * @param jti identificador del token
	 * @return {@code true} si ya no se acepta
	 */
	@Transactional(readOnly = true)
	public boolean estaRevocado(String jti) {
		return repositorio.existsByJti(jti);
	}
}
