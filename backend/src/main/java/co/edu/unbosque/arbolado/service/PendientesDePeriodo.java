package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.EstadoInscripcion;
import co.edu.unbosque.arbolado.repository.InscripcionRepository;
import org.springframework.stereotype.Component;

/**
 * Cuenta las inscripciones de un periodo que aún no tienen nota definitiva. Una
 * inscripción pasa de cursando a aprobada o reprobada al calcular la definitiva, así que las pendientes
 * son exactamente las que siguen cursando.
 */
@Component
public class PendientesDePeriodo {

	/** Repositorio de inscripciones. */
	private final InscripcionRepository inscripciones;

	/**
	 * Crea el componente.
	 *
	 * @param inscripciones repositorio de inscripciones
	 */
	public PendientesDePeriodo(InscripcionRepository inscripciones) {
		this.inscripciones = inscripciones;
	}

	/**
	 * Cuenta las inscripciones sin nota definitiva.
	 *
	 * @param periodoId identificador del periodo
	 * @return cantidad de inscripciones pendientes
	 */
	public long contarSinDefinitiva(Long periodoId) {
		return inscripciones.countByPeriodoIdAndEstado(periodoId, EstadoInscripcion.CURSANDO);
	}
}
