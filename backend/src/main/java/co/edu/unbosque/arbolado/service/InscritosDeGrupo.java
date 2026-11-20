package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.EstadoInscripcion;
import co.edu.unbosque.arbolado.repository.InscripcionRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Cuenta las inscripciones de los grupos: cupos ocupados y restricción de edición. */
@Component
public class InscritosDeGrupo {

	/** Repositorio de inscripciones. */
	private final InscripcionRepository inscripciones;

	/**
	 * Crea el componente.
	 *
	 * @param inscripciones repositorio de inscripciones
	 */
	public InscritosDeGrupo(InscripcionRepository inscripciones) {
		this.inscripciones = inscripciones;
	}

	/**
	 * Cuenta las inscripciones en estado CURSANDO, que son las que ocupan cupo.
	 *
	 * @param grupoIds grupos a consultar
	 * @return cantidad por grupo; los grupos sin inscritos faltan
	 */
	public Map<Long, Long> cursandoPorGrupo(Collection<Long> grupoIds) {
		Map<Long, Long> cantidades = new HashMap<>();
		if (grupoIds.isEmpty()) {
			return cantidades;
		}
		for (Object[] fila : inscripciones.cursandoPorGrupo(grupoIds)) {
			cantidades.put((Long) fila[0], (Long) fila[1]);
		}
		return cantidades;
	}

	/**
	 * Cuenta todas las inscripciones del grupo, sin importar su estado; con alguna, el grupo queda
	 * restringido.
	 *
	 * @param grupoId grupo a consultar
	 * @return cantidad de inscripciones
	 */
	public long contarInscripciones(Long grupoId) {
		return inscripciones.countByGrupoId(grupoId);
	}

	/**
	 * Cuenta las inscripciones cursando de un grupo.
	 *
	 * @param grupoId grupo a consultar
	 * @return estudiantes cursando
	 */
	public long contarCursando(Long grupoId) {
		return inscripciones.countByGrupoIdAndEstado(grupoId, EstadoInscripcion.CURSANDO);
	}
}
