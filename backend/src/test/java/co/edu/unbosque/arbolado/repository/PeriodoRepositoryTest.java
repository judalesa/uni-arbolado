package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Periodo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Comprueba que la base de datos misma impide dos periodos activos o repetidos (RN-13, RN-14). */
@SpringBootTest
@ActiveProfiles("test")
class PeriodoRepositoryTest {

	/** Repositorio de periodos. */
	@Autowired
	private PeriodoRepository periodos;

	/** Acceso directo a la base para dejar sin periodo activo. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Deja todos los periodos inactivos antes de cada prueba. */
	@BeforeEach
	void sinPeriodoActivo() {
		jdbc.update("update periodo set activo = false, marca_activo = null");
	}

	/**
	 * Crea un periodo con código propio de esta clase.
	 *
	 * @param codigo código del periodo
	 * @return el periodo sin guardar
	 */
	private static Periodo nuevo(String codigo) {
		return new Periodo(codigo, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30));
	}

	/** Dos periodos activos a la vez violan el índice único de la marca. */
	@Test
	void laBaseNoAdmiteDosPeriodosActivos() {
		Periodo a = nuevo("4000-1");
		a.activar();
		periodos.saveAndFlush(a);
		Periodo b = nuevo("4000-2");
		b.activar();

		assertThatThrownBy(() -> periodos.saveAndFlush(b)).isInstanceOf(DataIntegrityViolationException.class);
	}

	/** Varios periodos inactivos conviven: la marca nula no choca. */
	@Test
	void variosInactivosConviven() {
		periodos.saveAndFlush(nuevo("4001-1"));
		periodos.saveAndFlush(nuevo("4001-2"));

		assertThat(periodos.existsByCodigo("4001-1")).isTrue();
		assertThat(periodos.existsByCodigo("4001-2")).isTrue();
	}

	/** El código repetido viola el índice único. */
	@Test
	void laBaseNoAdmiteCodigosRepetidos() {
		periodos.saveAndFlush(nuevo("4002-1"));

		assertThatThrownBy(() -> periodos.saveAndFlush(nuevo("4002-1"))).isInstanceOf(DataIntegrityViolationException.class);
	}

	/** Desactivar libera la marca y permite activar otro. */
	@Test
	void desactivarLiberaLaMarca() {
		Periodo a = nuevo("4003-1");
		a.activar();
		periodos.saveAndFlush(a);
		a.desactivar();
		periodos.saveAndFlush(a);
		Periodo b = nuevo("4003-2");
		b.activar();
		periodos.saveAndFlush(b);

		assertThat(periodos.findByActivoTrue()).get().extracting(Periodo::getCodigo).isEqualTo("4003-2");
	}
}
