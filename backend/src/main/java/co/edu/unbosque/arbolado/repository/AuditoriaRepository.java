package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Auditoria;
import org.springframework.data.repository.Repository;

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
	 * Cuenta los registros guardados.
	 *
	 * @return cantidad de registros
	 */
	long count();
}
