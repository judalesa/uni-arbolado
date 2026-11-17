package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/** Acceso a datos de {@link TokenRevocado}. */
public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

	/**
	 * Indica si un token fue revocado.
	 *
	 * @param jti identificador del token
	 * @return {@code true} si está en la lista de revocados
	 */
	boolean existsByJti(String jti);

	/**
	 * Borra los registros de tokens que ya vencieron: un token vencido se rechaza igual.
	 *
	 * @param limite instante anterior al cual se borra
	 * @return cantidad de registros borrados
	 */
	@Modifying
	@Query("delete from TokenRevocado t where t.expiraEn < :limite")
	int purgarVencidos(@Param("limite") Instant limite);
}
