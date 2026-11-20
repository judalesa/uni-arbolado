package co.edu.unbosque.arbolado.service;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Cuenta las inscripciones de los grupos. Hasta que existan (FT-09) no hay ninguna, así que devuelve
 * cero; entonces solo cambian estas dos consultas.
 */
@Component
public class InscritosDeGrupo {

	/**
	 * Cuenta las inscripciones en estado CURSANDO, que son las que ocupan cupo (RN-11).
	 *
	 * @param grupoIds grupos a consultar
	 * @return cantidad por grupo; los grupos sin inscritos pueden faltar
	 */
	public Map<Long, Long> cursandoPorGrupo(Collection<Long> grupoIds) {
		return Map.of();
	}

	/**
	 * Cuenta todas las inscripciones del grupo, sin importar su estado; con alguna, el grupo queda
	 * restringido (A-07).
	 *
	 * @param grupoId grupo a consultar
	 * @return cantidad de inscripciones
	 */
	public long contarInscripciones(Long grupoId) {
		return 0L;
	}
}
