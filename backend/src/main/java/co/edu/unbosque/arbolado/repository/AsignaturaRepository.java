package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Asignatura;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link Asignatura}. */
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {

	/**
	 * Lista el plan completo con sus prerrequisitos en una sola consulta, ordenado por semestre y código.
	 *
	 * @return todas las asignaturas
	 */
	@EntityGraph(attributePaths = {"prerrequisitos", "prerrequisitos.requisito"})
	List<Asignatura> findAllByOrderBySemestreAscCodigoAsc();

	/**
	 * Busca una asignatura con sus prerrequisitos cargados.
	 *
	 * @param id identificador
	 * @return la asignatura, si existe
	 */
	@EntityGraph(attributePaths = {"prerrequisitos", "prerrequisitos.requisito"})
	Optional<Asignatura> findConPrerrequisitosById(Long id);

	/**
	 * Busca una asignatura por su código.
	 *
	 * @param codigo código, por ejemplo {@code MAT101}
	 * @return la asignatura, si existe
	 */
	Optional<Asignatura> findByCodigo(String codigo);
}
