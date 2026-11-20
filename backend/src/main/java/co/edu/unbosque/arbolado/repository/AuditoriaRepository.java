package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Auditoria;
import co.edu.unbosque.arbolado.model.Resultado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Acceso a datos de {@link Auditoria}. Extiende {@link Repository} y no {@code JpaRepository} a
 * propósito: solo expone guardar y consultar, sin ninguna operación de actualizar ni borrar.
 */
public interface AuditoriaRepository extends Repository<Auditoria, Long> {

	/**
	 * Guarda un registro nuevo.
	 *
	 * @param registro registro a guardar
	 * @return el registro guardado, con su identificador
	 */
	Auditoria save(Auditoria registro);

	/**
	 * Busca un registro por identificador.
	 *
	 * @param id identificador
	 * @return el registro, si existe
	 */
	Optional<Auditoria> findById(Long id);

	/**
	 * Busca registros con filtros opcionales; un filtro nulo no restringe. Se usa una consulta propia y no
	 * {@code JpaSpecificationExecutor} porque esa interfaz trae un método para borrar.
	 *
	 * @param usuarioId usuario que originó el evento
	 * @param accion    evento
	 * @param resultado exitoso o fallido
	 * @param desde     instante inicial, inclusive
	 * @param hasta     instante final, inclusive
	 * @param paginado  página y orden
	 * @return la página de registros
	 */
	@Query("select a from Auditoria a where (:usuarioId is null or a.usuarioId = :usuarioId) "
			+ "and (:accion is null or a.accion = :accion) and (:resultado is null or a.resultado = :resultado) "
			+ "and (:desde is null or a.fechaHora >= :desde) and (:hasta is null or a.fechaHora <= :hasta)")
	Page<Auditoria> buscar(@Param("usuarioId") Long usuarioId, @Param("accion") AccionAuditoria accion,
						   @Param("resultado") Resultado resultado, @Param("desde") Instant desde,
						   @Param("hasta") Instant hasta, Pageable paginado);

	/**
	 * Cuenta los registros guardados.
	 *
	 * @return cantidad de registros
	 */
	long count();
}
