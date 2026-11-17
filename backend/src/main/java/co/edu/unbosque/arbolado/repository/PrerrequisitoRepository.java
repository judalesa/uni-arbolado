package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.Prerrequisito;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de {@link Prerrequisito}. */
public interface PrerrequisitoRepository extends JpaRepository<Prerrequisito, Long> {

	/**
	 * Indica si ya existe la relación entre dos asignaturas.
	 *
	 * @param asignatura asignatura que exige el requisito
	 * @param requisito  asignatura requisito
	 * @return {@code true} si la relación existe
	 */
	boolean existsByAsignaturaAndRequisito(Asignatura asignatura, Asignatura requisito);
}
