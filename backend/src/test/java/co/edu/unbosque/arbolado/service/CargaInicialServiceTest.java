package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import co.edu.unbosque.arbolado.repository.PrerrequisitoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que la precarga deja el plan completo y es idempotente. */
@SpringBootTest
@ActiveProfiles("test")
class CargaInicialServiceTest {

	/** Servicio bajo prueba. */
	@Autowired
	private CargaInicialService servicio;

	/** Repositorio de asignaturas. */
	@Autowired
	private AsignaturaRepository asignaturas;

	/** Repositorio de prerrequisitos. */
	@Autowired
	private PrerrequisitoRepository prerrequisitos;

	/** Tras arrancar la aplicación ya están las 34 asignaturas y las 42 relaciones. */
	@Test
	void alArrancarQuedaElPlanCompleto() {
		assertThat(asignaturas.count()).isEqualTo(34);
		assertThat(prerrequisitos.count()).isEqualTo(42);
	}

	/** Ejecutar la carga otra vez no crea nada nuevo ni duplica registros. */
	@Test
	void cargarDeNuevoNoDuplicaNada() {
		CargaInicialService.ResultadoCarga resultado = servicio.cargarPlanDeEstudios();

		assertThat(resultado.asignaturasNuevas()).isZero();
		assertThat(resultado.prerrequisitosNuevos()).isZero();
		assertThat(asignaturas.count()).isEqualTo(34);
		assertThat(prerrequisitos.count()).isEqualTo(42);
	}

	/** Si falta una asignatura, la carga la repone sin tocar las demás. */
	@Test
	@Transactional
	void reponeUnaAsignaturaFaltante() {
		Asignatura aislada = asignaturas.findByCodigo("PVL901").orElseThrow();
		asignaturas.delete(aislada);
		asignaturas.flush();

		CargaInicialService.ResultadoCarga resultado = servicio.cargarPlanDeEstudios();

		assertThat(resultado.asignaturasNuevas()).isEqualTo(1);
		assertThat(asignaturas.count()).isEqualTo(34);
	}
}
