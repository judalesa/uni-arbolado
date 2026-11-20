package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.EstadoInscripcion;
import co.edu.unbosque.arbolado.model.Inscripcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Acceso a datos de {@link Inscripcion}. */
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

	/**
	 * Inscripciones de un estudiante en un estado, con su asignatura y su grupo con horarios, para validar
	 * créditos y traslapes sin consultas adicionales.
	 *
	 * @param estudianteId estudiante
	 * @param estado       estado a filtrar
	 * @return las inscripciones con su detalle
	 */
	@EntityGraph(attributePaths = {"asignatura", "grupo", "grupo.horarios"})
	List<Inscripcion> findByEstudianteIdAndEstado(Long estudianteId, EstadoInscripcion estado);

	/**
	 * Asignaturas que un estudiante tiene aprobadas.
	 *
	 * @param estudianteId estudiante
	 * @return identificadores de las asignaturas aprobadas
	 */
	@Query("select i.asignatura.id from Inscripcion i where i.estudiante.id = :estudianteId "
			+ "and i.estado = co.edu.unbosque.arbolado.model.EstadoInscripcion.APROBADA")
	Set<Long> asignaturasAprobadas(@Param("estudianteId") Long estudianteId);

	/**
	 * Cuenta las inscripciones de un grupo en un estado.
	 *
	 * @param grupoId grupo
	 * @param estado  estado a contar
	 * @return cantidad
	 */
	long countByGrupoIdAndEstado(Long grupoId, EstadoInscripcion estado);

	/**
	 * Cuenta todas las inscripciones de un grupo, sin importar su estado.
	 *
	 * @param grupoId grupo
	 * @return cantidad
	 */
	long countByGrupoId(Long grupoId);

	/**
	 * Cuenta las inscripciones de un periodo en un estado.
	 *
	 * @param periodoId periodo
	 * @param estado    estado a contar
	 * @return cantidad
	 */
	long countByPeriodoIdAndEstado(Long periodoId, EstadoInscripcion estado);

	/**
	 * Cuenta las inscripciones cursando de varios grupos con una sola consulta.
	 *
	 * @param grupoIds grupos a consultar
	 * @return filas {@code [grupoId, cantidad]}; los grupos sin inscritos no aparecen
	 */
	@Query("select i.grupo.id, count(i) from Inscripcion i where i.grupo.id in :grupoIds "
			+ "and i.estado = co.edu.unbosque.arbolado.model.EstadoInscripcion.CURSANDO group by i.grupo.id")
	List<Object[]> cursandoPorGrupo(@Param("grupoIds") Collection<Long> grupoIds);

	/**
	 * Inscripciones de un estudiante en un periodo, con todo lo que muestra el DTO en una sola sentencia.
	 *
	 * @param estudianteId estudiante
	 * @param periodoId    periodo
	 * @return las inscripciones ordenadas por código de asignatura
	 */
	@EntityGraph(attributePaths = {"estudiante", "grupo", "grupo.asignatura", "grupo.periodo", "grupo.profesor",
			"grupo.horarios"})
	List<Inscripcion> findByEstudianteIdAndPeriodoIdOrderByAsignaturaCodigoAsc(Long estudianteId, Long periodoId);

	/**
	 * Todos los intentos de un estudiante, del periodo más reciente al más antiguo.
	 *
	 * @param estudianteId estudiante
	 * @return las inscripciones con su detalle
	 */
	@EntityGraph(attributePaths = {"estudiante", "grupo", "grupo.asignatura", "grupo.periodo", "grupo.profesor",
			"grupo.horarios"})
	List<Inscripcion> findByEstudianteIdOrderByPeriodoCodigoDescAsignaturaCodigoAsc(Long estudianteId);

	/**
	 * Inscripciones de un grupo, en cualquier estado, ordenadas por nombre del estudiante.
	 *
	 * @param grupoId grupo
	 * @return las inscripciones con su detalle
	 */
	@EntityGraph(attributePaths = {"estudiante", "grupo", "grupo.asignatura", "grupo.periodo", "grupo.profesor",
			"grupo.horarios"})
	List<Inscripcion> findByGrupoIdOrderByEstudianteNombreCompletoAsc(Long grupoId);

	/**
	 * Indica si un estudiante tiene o tuvo alguna inscripción en un grupo de un profesor.
	 *
	 * @param estudianteId estudiante
	 * @param profesorId   profesor
	 * @return {@code true} si existe al menos una
	 */
	boolean existsByEstudianteIdAndGrupoProfesorId(Long estudianteId, Long profesorId);
}
