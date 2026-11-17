package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.AsignaturaDto;
import co.edu.unbosque.arbolado.dto.AsignaturaResumenDto;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de la consulta del plan de estudios. */
@SpringBootTest
@ActiveProfiles("test")
class AsignaturaServiceTest {

	/** Servicio bajo prueba. */
	@Autowired
	private AsignaturaService servicio;

	/** Repositorio, para obtener identificadores reales. */
	@Autowired
	private AsignaturaRepository repositorio;

	/** El listado trae las 34 asignaturas ordenadas por semestre y código. */
	@Test
	void listaElPlanOrdenadoPorSemestreYCodigo() {
		List<AsignaturaDto> plan = servicio.listar();

		assertThat(plan).hasSize(34);
		assertThat(plan.get(0).getCodigo()).isEqualTo("DSI101");
		assertThat(plan.get(33).getCodigo()).isEqualTo("TGR901");
		assertThat(plan).isSortedAccordingTo(
				java.util.Comparator.comparingInt(AsignaturaDto::getSemestre).thenComparing(AsignaturaDto::getCodigo));
	}

	/** Cada asignatura muestra sus prerrequisitos como asignaturas resumidas y ordenadas. */
	@Test
	void obtenerMuestraLosPrerrequisitos() {
		Long id = repositorio.findByCodigo("TGR901").orElseThrow().getId();

		AsignaturaDto trabajoDeGrado = servicio.obtener(id);

		assertThat(trabajoDeGrado.getNombre()).isEqualTo("Trabajo de Grado");
		assertThat(trabajoDeGrado.getCreditos()).isEqualTo(5);
		assertThat(trabajoDeGrado.getPrerrequisitos()).extracting(AsignaturaResumenDto::getCodigo)
				.containsExactly("DPI801", "PNU601");
		assertThat(trabajoDeGrado.getPrerrequisitos().get(0).getNombre()).isEqualTo("Diseño de Proyectos de Ingeniería");
	}

	/** Las asignaturas de primer semestre no tienen prerrequisitos. */
	@Test
	void unaAsignaturaInicialNoTienePrerrequisitos() {
		Long id = repositorio.findByCodigo("MAT101").orElseThrow().getId();

		assertThat(servicio.obtener(id).getPrerrequisitos()).isEmpty();
	}

	/** Pedir una asignatura inexistente lanza la excepción de "no encontrado". */
	@Test
	void unaAsignaturaInexistenteLanzaNoEncontrado() {
		assertThatThrownBy(() -> servicio.obtener(999_999L)).isInstanceOf(NoEncontradoException.class);
	}
}
