package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Grupo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link Grupo}. Las lecturas traen asignatura, periodo, profesor y horarios en una sentencia. */
public interface GrupoRepository extends JpaRepository<Grupo, Long> {

	/**
	 * Todos los grupos, del periodo más reciente al más antiguo.
	 *
	 * @return los grupos con su detalle
	 */
	@EntityGraph(attributePaths = {"asignatura", "periodo", "profesor", "horarios"})
	List<Grupo> findAllByOrderByPeriodoCodigoDescAsignaturaCodigoAscNumeroAsc();

	/**
	 * Grupos de un periodo.
	 *
	 * @param periodoId periodo
	 * @return los grupos con su detalle
	 */
	@EntityGraph(attributePaths = {"asignatura", "periodo", "profesor", "horarios"})
	List<Grupo> findByPeriodoIdOrderByAsignaturaCodigoAscNumeroAsc(Long periodoId);

	/**
	 * Grupos de un profesor en todos los periodos.
	 *
	 * @param profesorId profesor responsable
	 * @return los grupos con su detalle
	 */
	@EntityGraph(attributePaths = {"asignatura", "periodo", "profesor", "horarios"})
	List<Grupo> findByProfesorIdOrderByPeriodoCodigoDescAsignaturaCodigoAscNumeroAsc(Long profesorId);

	/**
	 * Grupos de un profesor en un periodo.
	 *
	 * @param profesorId profesor responsable
	 * @param periodoId  periodo
	 * @return los grupos con su detalle
	 */
	@EntityGraph(attributePaths = {"asignatura", "periodo", "profesor", "horarios"})
	List<Grupo> findByProfesorIdAndPeriodoIdOrderByAsignaturaCodigoAscNumeroAsc(Long profesorId, Long periodoId);

	/**
	 * Un grupo con todo su detalle.
	 *
	 * @param id identificador
	 * @return el grupo, si existe
	 */
	@EntityGraph(attributePaths = {"asignatura", "periodo", "profesor", "horarios"})
	Optional<Grupo> findConDetalleById(Long id);

	/**
	 * Un grupo con bloqueo de escritura: la edición y la inscripción se turnan sobre el mismo grupo.
	 *
	 * @param id identificador
	 * @return el grupo, si existe
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select g from Grupo g where g.id = :id")
	Optional<Grupo> findConBloqueoById(@Param("id") Long id);

	/**
	 * Siguiente número libre dentro de una asignatura y un periodo.
	 *
	 * @param asignaturaId asignatura
	 * @param periodoId    periodo
	 * @return el mayor número existente más uno, o 1 si no hay grupos
	 */
	@Query("select coalesce(max(g.numero), 0) + 1 from Grupo g where g.asignatura.id = :asignaturaId and g.periodo.id = :periodoId")
	int siguienteNumero(@Param("asignaturaId") Long asignaturaId, @Param("periodoId") Long periodoId);
}
