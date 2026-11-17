package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.Prerrequisito;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import co.edu.unbosque.arbolado.repository.PrerrequisitoRepository;
import co.edu.unbosque.arbolado.util.PlanDeEstudiosInicial;
import co.edu.unbosque.arbolado.util.PlanDeEstudiosInicial.AsignaturaSemilla;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/** Precarga idempotente de los datos base: se puede ejecutar varias veces sin duplicar nada. */
@Service
public class CargaInicialService {

	/**
	 * Cantidad de registros creados en una ejecución.
	 *
	 * @param asignaturasNuevas    asignaturas insertadas
	 * @param prerrequisitosNuevos relaciones de prerrequisito insertadas
	 * @param totalAsignaturas     asignaturas que hay en la base al terminar
	 * @param totalPrerrequisitos  relaciones de prerrequisito que hay en la base al terminar
	 */
	public record ResultadoCarga(int asignaturasNuevas, int prerrequisitosNuevos,
								 long totalAsignaturas, long totalPrerrequisitos) {
	}

	/** Repositorio de asignaturas. */
	private final AsignaturaRepository asignaturaRepository;

	/** Repositorio de prerrequisitos. */
	private final PrerrequisitoRepository prerrequisitoRepository;

	/**
	 * Crea el servicio.
	 *
	 * @param asignaturaRepository    repositorio de asignaturas
	 * @param prerrequisitoRepository repositorio de prerrequisitos
	 */
	public CargaInicialService(AsignaturaRepository asignaturaRepository,
							   PrerrequisitoRepository prerrequisitoRepository) {
		this.asignaturaRepository = asignaturaRepository;
		this.prerrequisitoRepository = prerrequisitoRepository;
	}

	/**
	 * Inserta las asignaturas y los prerrequisitos que aún no existan.
	 *
	 * @return cuántos registros nuevos se crearon (0 si todo ya estaba cargado)
	 */
	@Transactional
	public ResultadoCarga cargarPlanDeEstudios() {
		Map<String, Asignatura> porCodigo = new HashMap<>();
		asignaturaRepository.findAll().forEach(a -> porCodigo.put(a.getCodigo(), a));

		int asignaturasNuevas = 0;
		for (AsignaturaSemilla semilla : PlanDeEstudiosInicial.asignaturas()) {
			if (!porCodigo.containsKey(semilla.codigo())) {
				Asignatura nueva = asignaturaRepository.save(new Asignatura(semilla.codigo(), semilla.nombre(),
						semilla.descripcion(), semilla.semestre(), semilla.creditos()));
				porCodigo.put(nueva.getCodigo(), nueva);
				asignaturasNuevas++;
			}
		}

		int prerrequisitosNuevos = 0;
		for (AsignaturaSemilla semilla : PlanDeEstudiosInicial.asignaturas()) {
			Asignatura asignatura = porCodigo.get(semilla.codigo());
			for (String codigoRequisito : semilla.prerrequisitos()) {
				Asignatura requisito = porCodigo.get(codigoRequisito);
				if (!prerrequisitoRepository.existsByAsignaturaAndRequisito(asignatura, requisito)) {
					prerrequisitoRepository.save(new Prerrequisito(asignatura, requisito));
					prerrequisitosNuevos++;
				}
			}
		}
		return new ResultadoCarga(asignaturasNuevas, prerrequisitosNuevos,
				asignaturaRepository.count(), prerrequisitoRepository.count());
	}
}
