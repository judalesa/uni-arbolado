package co.edu.unbosque.arbolado.util;

import co.edu.unbosque.arbolado.util.PlanDeEstudiosInicial.AsignaturaSemilla;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida los datos del plan de estudios contra las cifras del documento del profesor. */
class PlanDeEstudiosInicialTest {

	/** Plan completo bajo prueba. */
	private final List<AsignaturaSemilla> plan = PlanDeEstudiosInicial.asignaturas();

	/** El plan tiene 34 asignaturas y 93 créditos en total. */
	@Test
	void tiene34AsignaturasY93Creditos() {
		assertThat(plan).hasSize(34);
		assertThat(plan.stream().mapToInt(AsignaturaSemilla::creditos).sum()).isEqualTo(93);
	}

	/** Hay 42 relaciones de prerrequisito en total. */
	@Test
	void tiene42Prerrequisitos() {
		assertThat(plan.stream().mapToInt(a -> a.prerrequisitos().size()).sum()).isEqualTo(42);
	}

	/** Los créditos por semestre coinciden con la tabla del documento. */
	@Test
	void losCreditosPorSemestreCoincidenConElDocumento() {
		Map<Integer, Integer> porSemestre = plan.stream().collect(
				Collectors.groupingBy(AsignaturaSemilla::semestre, Collectors.summingInt(AsignaturaSemilla::creditos)));
		assertThat(porSemestre).containsExactlyInAnyOrderEntriesOf(Map.of(
				1, 12, 2, 11, 3, 11, 4, 11, 5, 12, 6, 11, 7, 11, 8, 7, 9, 7));
	}

	/** Códigos y nombres son únicos y los valores respetan los rangos del modelo. */
	@Test
	void codigosYNombresSonUnicosYLosRangosSeRespetan() {
		assertThat(plan.stream().map(AsignaturaSemilla::codigo).collect(Collectors.toSet())).hasSize(34);
		assertThat(plan.stream().map(AsignaturaSemilla::nombre).collect(Collectors.toSet())).hasSize(34);
		for (AsignaturaSemilla a : plan) {
			assertThat(a.semestre()).as(a.codigo()).isBetween(1, 9);
			assertThat(a.creditos()).as(a.codigo()).isBetween(1, 5);
			assertThat(a.descripcion()).as(a.codigo()).isNotBlank().hasSizeLessThanOrEqualTo(500);
		}
	}

	/** Todo prerrequisito existe y pertenece a un semestre anterior. */
	@Test
	void todoPrerrequisitoExisteYEsDeUnSemestreAnterior() {
		Map<String, AsignaturaSemilla> porCodigo = new HashMap<>();
		plan.forEach(a -> porCodigo.put(a.codigo(), a));
		for (AsignaturaSemilla a : plan) {
			for (String codigo : a.prerrequisitos()) {
				assertThat(porCodigo).as("%s exige %s", a.codigo(), codigo).containsKey(codigo);
				assertThat(porCodigo.get(codigo).semestre()).as("%s exige %s", a.codigo(), codigo)
						.isLessThan(a.semestre());
			}
			assertThat(a.prerrequisitos()).doesNotHaveDuplicates();
		}
	}

	/** El grafo de prerrequisitos no tiene ciclos (se puede ordenar topológicamente). */
	@Test
	void elPlanNoTieneCiclos() {
		Map<String, Integer> pendientes = new HashMap<>();
		Map<String, List<String>> dependientes = new HashMap<>();
		for (AsignaturaSemilla a : plan) {
			pendientes.put(a.codigo(), a.prerrequisitos().size());
			for (String requisito : a.prerrequisitos()) {
				dependientes.computeIfAbsent(requisito, k -> new java.util.ArrayList<>()).add(a.codigo());
			}
		}
		Deque<String> cola = new ArrayDeque<>();
		pendientes.forEach((codigo, n) -> {
			if (n == 0) {
				cola.add(codigo);
			}
		});
		Set<String> ordenadas = new java.util.HashSet<>();
		while (!cola.isEmpty()) {
			String actual = cola.poll();
			ordenadas.add(actual);
			for (String dependiente : dependientes.getOrDefault(actual, List.of())) {
				if (pendientes.merge(dependiente, -1, Integer::sum) == 0) {
					cola.add(dependiente);
				}
			}
		}
		assertThat(ordenadas).hasSize(34);
	}
}
