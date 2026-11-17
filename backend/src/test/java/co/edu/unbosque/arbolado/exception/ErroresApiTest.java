package co.edu.unbosque.arbolado.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta del contrato de errores: cada situación debe responder con el estado HTTP y el
 * cuerpo {@code ErrorResponse} que promete {@code openapi.yaml}, pasando por la cadena de seguridad real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErroresApiTest {

	/** Cliente HTTP simulado que atraviesa la cadena de seguridad real sin abrir un puerto. */
	@Autowired
	private MockMvc mvc;

	/** Una petición sin token recibe 401 con el formato {@code ErrorResponse} del contrato. */
	@Test
	void sinTokenResponde401ConFormatoDelContrato() throws Exception {
		mvc.perform(get("/api/prueba/no-encontrado"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
				.andExpect(jsonPath("$.ruta").value("/api/prueba/no-encontrado"))
				.andExpect(jsonPath("$.timestamp").value(org.hamcrest.Matchers.endsWith("Z")))
				.andExpect(jsonPath("$.detalles").doesNotExist())
				.andExpect(jsonPath("$.motivos").doesNotExist());
	}

	/** Un recurso inexistente responde 404 {@code NO_ENCONTRADO} con mensaje estándar. */
	@Test
	void recursoInexistenteResponde404() throws Exception {
		mvc.perform(get("/api/prueba/no-encontrado").with(user("ana")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
				.andExpect(jsonPath("$.mensaje").value("La asignatura con identificador 7 no existe"));
	}

	/** Un conflicto de datos responde 409 con su código específico. */
	@Test
	void conflictoResponde409ConSuCodigo() throws Exception {
		mvc.perform(get("/api/prueba/conflicto").with(user("ana")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DOCUMENTO_DUPLICADO"));
	}

	/** Una regla de negocio incumplida responde 422 con todos los motivos (RN-22). */
	@Test
	void reglaDeNegocioResponde422ConTodosLosMotivos() throws Exception {
		mvc.perform(get("/api/prueba/regla").with(user("ana")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"))
				.andExpect(jsonPath("$.motivos", hasSize(2)))
				.andExpect(jsonPath("$.motivos[0].codigo").value("CREDITOS_EXCEDIDOS"))
				.andExpect(jsonPath("$.motivos[1].codigo").value("HORARIO_TRASLAPADO"));
	}

	/** Demasiados intentos responden 429 con la cabecera {@code Retry-After}. */
	@Test
	void demasiadosIntentosResponde429ConRetryAfter() throws Exception {
		mvc.perform(get("/api/prueba/demasiados").with(user("ana")))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
				.andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"));
	}

	/** Un cuerpo inválido responde 400 con un detalle por cada campo incorrecto. */
	@Test
	void cuerpoInvalidoResponde400ConUnDetallePorCampo() throws Exception {
		mvc.perform(post("/api/prueba/validar").with(user("ana"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"\",\"correo\":\"no-es-correo\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.detalles", hasSize(2)))
				.andExpect(jsonPath("$.detalles[*].campo").value(org.hamcrest.Matchers.containsInAnyOrder("nombre", "correo")));
	}

	/** Un JSON mal formado responde 400 {@code VALIDACION}. */
	@Test
	void jsonMalFormadoResponde400() throws Exception {
		mvc.perform(post("/api/prueba/validar").with(user("ana"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{esto no es json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	/** Un parámetro de ruta con tipo incorrecto responde 400 indicando el campo. */
	@Test
	void parametroConTipoIncorrectoResponde400IndicandoElCampo() throws Exception {
		mvc.perform(get("/api/prueba/numero/abc").with(user("ana")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detalles[0].campo").value("id"));
	}

	/** Una ruta que no existe responde 404 en el formato del contrato. */
	@Test
	void rutaInexistenteResponde404EnFormatoDelContrato() throws Exception {
		mvc.perform(get("/api/no-existe").with(user("ana")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	/** Un método HTTP no permitido responde 405. */
	@Test
	void metodoNoPermitidoResponde405() throws Exception {
		mvc.perform(put("/api/prueba/conflicto").with(user("ana")))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
	}

	/** Un cuerpo que no es JSON responde 415. */
	@Test
	void tipoDeContenidoNoSoportadoResponde415() throws Exception {
		mvc.perform(post("/api/prueba/validar").with(user("ana"))
						.contentType(MediaType.TEXT_PLAIN)
						.content("hola"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.codigo").value("TIPO_NO_SOPORTADO"));
	}

	/** Un error inesperado responde 500 genérico, sin filtrar detalles internos al cliente. */
	@Test
	void errorInesperadoResponde500SinFiltrarDetallesInternos() throws Exception {
		mvc.perform(get("/api/prueba/inesperado").with(user("ana")))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
				.andExpect(content().string(not(containsString("abc123"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	/** Un usuario sin el rol requerido recibe 403 {@code ACCESO_DENEGADO}. */
	@Test
	void rolInsuficienteResponde403() throws Exception {
		mvc.perform(get("/api/prueba/solo-admin").with(user("ana").roles("ESTUDIANTE")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	/** Un usuario con el rol requerido sí accede. */
	@Test
	void rolSuficientePermiteElAcceso() throws Exception {
		mvc.perform(get("/api/prueba/solo-admin").with(user("root").roles("ADMINISTRADOR")))
				.andExpect(status().isOk());
	}

	/** CORS acepta la verificación previa de un origen configurado. */
	@Test
	void corsPermiteUnOrigenConfigurado() throws Exception {
		mvc.perform(options("/api/prueba/conflicto")
						.header(HttpHeaders.ORIGIN, "https://sitio-permitido.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://sitio-permitido.example.com"));
	}

	/** CORS rechaza un origen que no está en la configuración. */
	@Test
	void corsRechazaUnOrigenNoConfigurado() throws Exception {
		mvc.perform(options("/api/prueba/conflicto")
						.header(HttpHeaders.ORIGIN, "https://sitio-malicioso.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	/** Con Swagger encendido, {@code /v3/api-docs} responde sin token. */
	@Test
	void laDocumentacionOpenApiEstaDisponibleSinTokenCuandoSeEnciende() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value(org.hamcrest.Matchers.containsString("Universidad El Arbolado")));
	}
}
