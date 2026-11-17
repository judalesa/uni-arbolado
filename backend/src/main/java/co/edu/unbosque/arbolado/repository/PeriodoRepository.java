package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Periodo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link Periodo}. */
public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

	/**
	 * Indica si ya existe un periodo con ese código.
	 *
	 * @param codigo código con formato AAAA-N
	 * @return {@code true} si existe
	 */
	boolean existsByCodigo(String codigo);

	/**
	 * Indica si otro periodo distinto del dado usa ese código.
	 *
	 * @param codigo código con formato AAAA-N
	 * @param id     periodo que se excluye de la comparación
	 * @return {@code true} si lo usa otro periodo
	 */
	boolean existsByCodigoAndIdNot(String codigo, Long id);

	/**
	 * Todos los periodos, del más reciente al más antiguo por código.
	 *
	 * @return la lista ordenada
	 */
	List<Periodo> findAllByOrderByCodigoDesc();

	/**
	 * Periodo activo, si lo hay.
	 *
	 * @return el periodo activo
	 */
	Optional<Periodo> findByActivoTrue();

	/**
	 * Periodo activo con bloqueo de escritura: dos activaciones simultáneas se turnan y la segunda ve el
	 * resultado de la primera.
	 *
	 * @return el periodo activo
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Periodo> findFirstByActivoTrue();
}
