package co.edu.unbosque.arbolado.service;

import org.springframework.stereotype.Component;

/**
 * Cuenta las inscripciones de un periodo que aún no tienen nota definitiva (decisión A-02). Hasta que
 * existan inscripciones (FT-09 y FT-11) no hay nada pendiente; entonces solo cambia esta consulta.
 */
@Component
public class PendientesDePeriodo {

	/**
	 * Cuenta las inscripciones sin nota definitiva.
	 *
	 * @param periodoId identificador del periodo
	 * @return cantidad de inscripciones pendientes
	 */
	public long contarSinDefinitiva(Long periodoId) {
		return 0L;
	}
}
