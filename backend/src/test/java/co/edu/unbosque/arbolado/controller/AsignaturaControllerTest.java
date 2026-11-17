package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas del endpoint de consulta del plan de estudios, con la cadena de seguridad real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AsignaturaControllerTest {

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mvc;

	/** Repositorio, para obtener identificadores reales. */
	@Autowired
	private AsignaturaRepository repositorio;

	/** Un estudiante recibe las 34 asignaturas con sus prerrequisitos. */
	@Test
	void unEstudianteConsultaElPlanCompleto() throws Exception {
		mvc.perform(get("/api/asignaturas").with(user("ana").roles("ESTUDIANTE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(34)))
				.andExpect(jsonPath("$[0].codigo").value("DSI101"))
				.andExpect(jsonPath("$[0].prerrequisitos", hasSize(0)));
	}

	/** Un administrador también puede consultar el plan. */
	@Test
	void unAdministradorConsultaElPlan() throws Exception {
		mvc.perform(get("/api/asignaturas").with(user("root").roles("ADMINISTRADOR")))
				.andExpect(status().isOk());
	}

	/** Un profesor no tiene acceso a esta consulta. */
	@Test
	void unProfesorRecibe403() throws Exception {
		mvc.perform(get("/api/asignaturas").with(user("profe").roles("PROFESOR")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	/** Sin token se responde 401. */
	@Test
	void sinTokenRecibe401() throws Exception {
		mvc.perform(get("/api/asignaturas"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
	}

	/** Una asignatura se consulta por identificador con la forma del contrato. */
	@Test
	void consultaUnaAsignaturaPorId() throws Exception {
		Long id = repositorio.findByCodigo("SIN801").orElseThrow().getId();

		mvc.perform(get("/api/asignaturas/{id}", id).with(user("ana").roles("ESTUDIANTE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.codigo").value("SIN801"))
				.andExpect(jsonPath("$.semestre").value(8))
				.andExpect(jsonPath("$.creditos").value(2))
				.andExpect(jsonPath("$.descripcion").isNotEmpty())
				.andExpect(jsonPath("$.prerrequisitos", hasSize(3)))
				.andExpect(jsonPath("$.prerrequisitos[0].codigo").value("ALG201"));
	}

	/** Un identificador inexistente responde 404 con el formato del contrato. */
	@Test
	void unaAsignaturaInexistenteRecibe404() throws Exception {
		mvc.perform(get("/api/asignaturas/{id}", 999_999).with(user("ana").roles("ESTUDIANTE")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	/** Un identificador que no es número responde 400. */
	@Test
	void unIdentificadorNoNumericoRecibe400() throws Exception {
		mvc.perform(get("/api/asignaturas/abc").with(user("ana").roles("ESTUDIANTE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	/** El plan es de solo lectura: no existe ningún método de escritura (RN-08). */
	@Test
	void noSePuedeEscribirElPlan() throws Exception {
		mvc.perform(post("/api/asignaturas").with(user("root").roles("ADMINISTRADOR"))
						.contentType("application/json").content("{}"))
				.andExpect(status().isMethodNotAllowed());
	}
}
