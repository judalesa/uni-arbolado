package co.edu.unbosque.arbolado.controller;

import com.jayway.jsonpath.JsonPath;
import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import co.edu.unbosque.arbolado.repository.PeriodoRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.JwtService;
import co.edu.unbosque.arbolado.service.InscritosDeGrupo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de grupos y horarios con la cadena de seguridad real y el conteo de inscritos simulado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GrupoControllerTest {

	/** Clave de las cuentas de prueba. */
	private static final String CLAVE = "ClaveSegura1";

	/** Franja de lunes de 8 a 10. */
	private static final String LUNES_8_10 = "{\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}";

	/** Franja de martes de 10 a 12. */
	private static final String MARTES_10_12 = "{\"diaSemana\":\"MARTES\",\"horaInicio\":\"10:00\",\"horaFin\":\"12:00\"}";

	/** Contador de documentos únicos de las cuentas. */
	private static final AtomicLong DOCUMENTOS = new AtomicLong(6_000_000_000L);

	/** Contador de códigos de periodo únicos (5000-0, 5000-1, …). */
	private static final AtomicInteger CODIGOS = new AtomicInteger();

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mvc;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Repositorio de periodos. */
	@Autowired
	private PeriodoRepository periodos;

	/** Repositorio de asignaturas del plan. */
	@Autowired
	private AsignaturaRepository asignaturas;

	/** Repositorio de auditoría. */
	@Autowired
	private AuditoriaRepository auditoria;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Emisor de tokens. */
	@Autowired
	private JwtService jwt;

	/** Acceso directo a la base para dejar sin periodo activo. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Conteo de inscripciones simulado. */
	@MockitoBean
	private InscritosDeGrupo inscritos;

	/** Administrador que actúa en las pruebas de administración. */
	private String admin;

	/** Deja todos los periodos inactivos y crea el administrador de la prueba. */
	@BeforeEach
	void preparar() {
		jdbc.update("update periodo set activo = false, marca_activo = null");
		admin = bearer(cuenta(Rol.ADMINISTRADOR));
	}

	/**
	 * Guarda una cuenta con documento y correo únicos.
	 *
	 * @param rol rol de la cuenta
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol) {
		long n = DOCUMENTOS.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), "Cuenta " + rol, "g" + n + "@example.com",
				codificador.encode(CLAVE), rol, EstadoCuenta.ACTIVO));
	}

	/**
	 * Encabezado de autorización de una cuenta.
	 *
	 * @param usuario cuenta que actúa
	 * @return valor del encabezado {@code Authorization}
	 */
	private String bearer(Usuario usuario) {
		return "Bearer " + jwt.emitir(usuario).token();
	}

	/**
	 * Guarda un periodo inactivo con código único.
	 *
	 * @return el periodo
	 */
	private Periodo periodo() {
		int n = CODIGOS.getAndIncrement();
		return periodos.save(new Periodo((5000 + n / 10) + "-" + (n % 10), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30)));
	}

	/**
	 * Busca una asignatura del plan.
	 *
	 * @param codigo código, por ejemplo {@code MAT101}
	 * @return la asignatura
	 */
	private Asignatura asignatura(String codigo) {
		return asignaturas.findByCodigo(codigo).orElseThrow();
	}

	/**
	 * Cuerpo JSON de un grupo.
	 *
	 * @param asignatura asignatura
	 * @param periodo    periodo
	 * @param profesor   profesor responsable
	 * @param horarios   horarios ya en JSON, separados por coma
	 * @return el JSON
	 */
	private static String cuerpo(Asignatura asignatura, Periodo periodo, Usuario profesor, String horarios) {
		return """
				{"asignaturaId":%d,"periodoId":%d,"profesorId":%d,"bloque":"B","salon":"301","horarios":[%s]}
				""".formatted(asignatura.getId(), periodo.getId(), profesor.getId(), horarios);
	}

	/**
	 * Crea un grupo por la API y devuelve su respuesta.
	 *
	 * @param cuerpo JSON del grupo
	 * @return la respuesta
	 * @throws Exception si falla la petición
	 */
	private ResultActions crear(String cuerpo) throws Exception {
		return mvc.perform(post("/api/admin/grupos").header(HttpHeaders.AUTHORIZATION, admin)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	/**
	 * Crea un grupo válido y devuelve su identificador.
	 *
	 * @param asignatura asignatura
	 * @param periodo    periodo
	 * @param profesor   profesor
	 * @return el identificador del grupo creado
	 * @throws Exception si falla la petición
	 */
	private long grupoCreado(Asignatura asignatura, Periodo periodo, Usuario profesor) throws Exception {
		String respuesta = crear(cuerpo(asignatura, periodo, profesor, LUNES_8_10)).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.parse(respuesta).read("$.id", Long.class);
	}

	/** Un grupo nuevo recibe el número 1, cupo 30 y horarios ordenados por día; el siguiente recibe el 2. */
	@Test
	void creaGruposConNumeroAutomaticoYCupo30() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Asignatura mat = asignatura("MAT101");
		long antes = auditoria.count();

		crear(cuerpo(mat, periodo, profesor, MARTES_10_12 + "," + LUNES_8_10))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.numero").value(1))
				.andExpect(jsonPath("$.cupoMaximo").value(30))
				.andExpect(jsonPath("$.inscritos").value(0))
				.andExpect(jsonPath("$.cuposDisponibles").value(30))
				.andExpect(jsonPath("$.asignatura.codigo").value("MAT101"))
				.andExpect(jsonPath("$.periodo.codigo").value(periodo.getCodigo()))
				.andExpect(jsonPath("$.profesor.id").value(profesor.getId()))
				.andExpect(jsonPath("$.horarios[0].diaSemana").value("LUNES"))
				.andExpect(jsonPath("$.horarios[0].horaInicio").value("08:00"))
				.andExpect(jsonPath("$.horarios[1].diaSemana").value("MARTES"));
		crear(cuerpo(mat, periodo, profesor, LUNES_8_10)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.numero").value(2));
		crear(cuerpo(mat, periodo(), profesor, LUNES_8_10)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.numero").value(1));

		assertThat(auditoria.count()).isEqualTo(antes + 3);
	}

	/** El cupo no se puede imponer desde el cuerpo: siempre es 30. */
	@Test
	void elCupoNoSePuedeEscribir() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		String conCupo = cuerpo(asignatura("MAT101"), periodo, profesor, LUNES_8_10).replace("\"bloque\"", "\"cupoMaximo\":99,\"bloque\"");

		crear(conCupo).andExpect(status().isCreated()).andExpect(jsonPath("$.cupoMaximo").value(30));
	}

	/** Datos mal formados responden 400: sin horarios, hora inválida, día inexistente, bloque vacío. */
	@Test
	void losDatosInvalidosResponden400() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Asignatura mat = asignatura("MAT101");
		List<String> cuerpos = List.of(
				cuerpo(mat, periodo, profesor, ""),
				cuerpo(mat, periodo, profesor, "{\"diaSemana\":\"LUNES\",\"horaInicio\":\"8:00\",\"horaFin\":\"10:00\"}"),
				cuerpo(mat, periodo, profesor, "{\"diaSemana\":\"LUNES\",\"horaInicio\":\"08:00\",\"horaFin\":\"25:00\"}"),
				cuerpo(mat, periodo, profesor, "{\"diaSemana\":\"DOMINGO\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}"),
				cuerpo(mat, periodo, profesor, LUNES_8_10).replace("\"bloque\":\"B\"", "\"bloque\":\" \""),
				cuerpo(mat, periodo, profesor, LUNES_8_10).replace("\"salon\":\"301\"", "\"salon\":\"" + "x".repeat(21) + "\""));

		for (String cuerpo : cuerpos) {
			crear(cuerpo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
		}
	}

	/** Un horario que termina antes de empezar, o dos que se pisan, responden 422 y quedan auditados. */
	@Test
	void horariosInvalidosResponden422() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Asignatura mat = asignatura("MAT101");
		long antes = auditoria.count();

		crear(cuerpo(mat, periodo, profesor, "{\"diaSemana\":\"LUNES\",\"horaInicio\":\"10:00\",\"horaFin\":\"08:00\"}"))
				.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
		crear(cuerpo(mat, periodo, profesor, "{\"diaSemana\":\"LUNES\",\"horaInicio\":\"10:00\",\"horaFin\":\"10:00\"}"))
				.andExpect(status().isUnprocessableEntity());
		crear(cuerpo(mat, periodo, profesor, LUNES_8_10 + ",{\"diaSemana\":\"LUNES\",\"horaInicio\":\"09:00\",\"horaFin\":\"11:00\"}"))
				.andExpect(status().isUnprocessableEntity());

		assertThat(auditoria.count()).isEqualTo(antes + 3);
	}

	/** Empezar justo cuando termina otra franja del mismo día no es traslape, y el mismo horario otro día tampoco. */
	@Test
	void franjasContiguasNoSeTraslapan() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);

		crear(cuerpo(asignatura("MAT101"), periodo, profesor, LUNES_8_10
				+ ",{\"diaSemana\":\"LUNES\",\"horaInicio\":\"10:00\",\"horaFin\":\"12:00\"}"
				+ ",{\"diaSemana\":\"MARTES\",\"horaInicio\":\"08:00\",\"horaFin\":\"10:00\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.horarios", hasSize(3)));
	}

	/** Un responsable que no es profesor responde 422; asignatura, periodo o profesor inexistentes, 404. */
	@Test
	void responsableYReferenciasInvalidos() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Asignatura mat = asignatura("MAT101");

		crear(cuerpo(mat, periodo, cuenta(Rol.ESTUDIANTE), LUNES_8_10)).andExpect(status().isUnprocessableEntity());
		crear(cuerpo(mat, periodo, cuenta(Rol.ADMINISTRADOR), LUNES_8_10)).andExpect(status().isUnprocessableEntity());
		crear(cuerpo(mat, periodo, profesor, LUNES_8_10).replace("\"profesorId\":" + profesor.getId(), "\"profesorId\":999999999"))
				.andExpect(status().isNotFound());
		crear(cuerpo(mat, periodo, profesor, LUNES_8_10).replace("\"periodoId\":" + periodo.getId(), "\"periodoId\":999999999"))
				.andExpect(status().isNotFound());
		crear(cuerpo(mat, periodo, profesor, LUNES_8_10).replace("\"asignaturaId\":" + mat.getId(), "\"asignaturaId\":999999999"))
				.andExpect(status().isNotFound());
	}

	/** Sin inscritos se puede cambiar todo: asignatura, periodo y horarios; al cambiar de oferta el número se reasigna. */
	@Test
	void sinInscritosSePuedeEditarTodo() throws Exception {
		Periodo periodo = periodo();
		Periodo otro = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Usuario relevo = cuenta(Rol.PROFESOR);
		long id = grupoCreado(asignatura("MAT101"), periodo, profesor);
		grupoCreado(asignatura("MAT101"), otro, profesor);

		mvc.perform(put("/api/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo(asignatura("MAT101"), otro, relevo, MARTES_10_12)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numero").value(2))
				.andExpect(jsonPath("$.periodo.id").value(otro.getId()))
				.andExpect(jsonPath("$.profesor.id").value(relevo.getId()))
				.andExpect(jsonPath("$.horarios", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].diaSemana").value("MARTES"));
	}

	/** Con inscritos solo cambian profesor, bloque y salón; cambiar asignatura, periodo u horarios responde 422. */
	@Test
	void conInscritosSoloSePuedeCambiarProfesorBloqueYSalon() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Usuario relevo = cuenta(Rol.PROFESOR);
		Asignatura mat = asignatura("MAT101");
		long id = grupoCreado(mat, periodo, profesor);
		when(inscritos.contarInscripciones(id)).thenReturn(5L);
		long antes = auditoria.count();

		mvc.perform(put("/api/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo(mat, periodo, relevo, LUNES_8_10).replace("\"salon\":\"301\"", "\"salon\":\"502\"")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profesor.id").value(relevo.getId()))
				.andExpect(jsonPath("$.salon").value("502"))
				.andExpect(jsonPath("$.numero").value(1));

		for (String bloqueado : List.of(
				cuerpo(mat, periodo, profesor, MARTES_10_12),
				cuerpo(mat, periodo, profesor, LUNES_8_10 + "," + MARTES_10_12),
				cuerpo(asignatura("ALG201"), periodo, profesor, LUNES_8_10),
				cuerpo(mat, periodo(), profesor, LUNES_8_10))) {
			mvc.perform(put("/api/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
							.contentType(MediaType.APPLICATION_JSON).content(bloqueado))
					.andExpect(status().isUnprocessableEntity())
					.andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));
		}
		mvc.perform(get("/api/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.horarios", hasSize(1)))
				.andExpect(jsonPath("$.horarios[0].diaSemana").value("LUNES"));
		assertThat(auditoria.count()).isEqualTo(antes + 5);
	}

	/** Enviar los mismos horarios en otro orden no cuenta como cambio y se acepta con inscritos. */
	@Test
	void losMismosHorariosEnOtroOrdenSeAceptanConInscritos() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		Asignatura mat = asignatura("MAT101");
		String respuesta = crear(cuerpo(mat, periodo, profesor, LUNES_8_10 + "," + MARTES_10_12))
				.andReturn().getResponse().getContentAsString();
		long id = JsonPath.parse(respuesta).read("$.id", Long.class);
		when(inscritos.contarInscripciones(id)).thenReturn(1L);

		mvc.perform(put("/api/admin/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo(mat, periodo, profesor, MARTES_10_12 + "," + LUNES_8_10)))
				.andExpect(status().isOk());
	}

	/** Editar un grupo inexistente responde 404. */
	@Test
	void editarInexistenteResponde404() throws Exception {
		mvc.perform(put("/api/admin/grupos/{id}", 999_999_999).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpo(asignatura("MAT101"), periodo(), cuenta(Rol.PROFESOR), LUNES_8_10)))
				.andExpect(status().isNotFound());
	}

	/** Los cupos disponibles salen del conteo de estudiantes cursando y nunca bajan de cero. */
	@Test
	void calculaLosCuposDisponibles() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		long casiLleno = grupoCreado(asignatura("MAT101"), periodo, profesor);
		long lleno = grupoCreado(asignatura("ALG201"), periodo, profesor);
		long excedido = grupoCreado(asignatura("DSI101"), periodo, profesor);
		when(inscritos.cursandoPorGrupo(any())).thenReturn(Map.of(casiLleno, 28L, lleno, 30L, excedido, 35L));

		mvc.perform(get("/api/admin/grupos/{id}", casiLleno).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.inscritos").value(28))
				.andExpect(jsonPath("$.cuposDisponibles").value(2));
		mvc.perform(get("/api/admin/grupos/{id}", lleno).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.cuposDisponibles").value(0));
		mvc.perform(get("/api/admin/grupos/{id}", excedido).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.cuposDisponibles").value(0));
	}

	/** El listado del administrador filtra por periodo; un grupo inexistente responde 404. */
	@Test
	void listaYFiltraPorPeriodo() throws Exception {
		Periodo uno = periodo();
		Periodo dos = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		long enUno = grupoCreado(asignatura("MAT101"), uno, profesor);
		grupoCreado(asignatura("MAT101"), dos, profesor);

		mvc.perform(get("/api/admin/grupos").param("periodoId", String.valueOf(uno.getId())).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(enUno));
		mvc.perform(get("/api/admin/grupos").header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk());
		mvc.perform(get("/api/admin/grupos/{id}", 999_999_999).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isNotFound());
	}

	/** La oferta del estudiante trae solo los grupos del periodo activo; sin periodo activo, lista vacía. */
	@Test
	void laOfertaEsDelPeriodoActivo() throws Exception {
		Periodo activo = periodo();
		Periodo otro = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		String estudiante = bearer(cuenta(Rol.ESTUDIANTE));
		long enActivo = grupoCreado(asignatura("MAT101"), activo, profesor);
		grupoCreado(asignatura("MAT101"), otro, profesor);

		mvc.perform(get("/api/grupos/oferta").header(HttpHeaders.AUTHORIZATION, estudiante))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

		mvc.perform(post("/api/periodos/{id}/activar", activo.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk());
		mvc.perform(get("/api/grupos/oferta").header(HttpHeaders.AUTHORIZATION, estudiante))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(enActivo))
				.andExpect(jsonPath("$[0].cuposDisponibles").value(30));
	}

	/** Cada profesor ve solo sus grupos y puede filtrarlos por periodo. */
	@Test
	void cadaProfesorVeSoloSusGrupos() throws Exception {
		Periodo uno = periodo();
		Periodo dos = periodo();
		Usuario ana = cuenta(Rol.PROFESOR);
		Usuario beto = cuenta(Rol.PROFESOR);
		long deAnaUno = grupoCreado(asignatura("MAT101"), uno, ana);
		grupoCreado(asignatura("MAT101"), dos, ana);
		grupoCreado(asignatura("ALG201"), uno, beto);

		mvc.perform(get("/api/profesor/grupos").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		mvc.perform(get("/api/profesor/grupos").param("periodoId", String.valueOf(uno.getId()))
						.header(HttpHeaders.AUTHORIZATION, bearer(ana)))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(deAnaUno));
		mvc.perform(get("/api/profesor/grupos").header(HttpHeaders.AUTHORIZATION, bearer(beto)))
				.andExpect(jsonPath("$", hasSize(1)));
	}

	/** Cada ruta responde 403 a los roles que no le corresponden y 401 sin token. */
	@Test
	void permisosPorRol() throws Exception {
		Periodo periodo = periodo();
		Usuario profesor = cuenta(Rol.PROFESOR);
		long id = grupoCreado(asignatura("MAT101"), periodo, profesor);
		String cuerpo = cuerpo(asignatura("MAT101"), periodo, profesor, LUNES_8_10);
		Map<String, List<Rol>> prohibidos = Map.of(
				"admin", List.of(Rol.ESTUDIANTE, Rol.PROFESOR),
				"oferta", List.of(Rol.PROFESOR, Rol.ADMINISTRADOR),
				"profesor", List.of(Rol.ESTUDIANTE, Rol.ADMINISTRADOR));
		Map<String, List<Supplier<MockHttpServletRequestBuilder>>> rutas = Map.of(
				"admin", List.of(
						() -> get("/api/admin/grupos"),
						() -> post("/api/admin/grupos").contentType(MediaType.APPLICATION_JSON).content(cuerpo),
						() -> get("/api/admin/grupos/{id}", id),
						() -> put("/api/admin/grupos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(cuerpo)),
				"oferta", List.of(() -> get("/api/grupos/oferta")),
				"profesor", List.of(() -> get("/api/profesor/grupos")));

		for (Map.Entry<String, List<Supplier<MockHttpServletRequestBuilder>>> ruta : rutas.entrySet()) {
			for (Rol rol : prohibidos.get(ruta.getKey())) {
				String token = bearer(cuenta(rol));
				for (Supplier<MockHttpServletRequestBuilder> peticion : ruta.getValue()) {
					mvc.perform(peticion.get().header(HttpHeaders.AUTHORIZATION, token))
							.andExpect(status().isForbidden())
							.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
				}
			}
			for (Supplier<MockHttpServletRequestBuilder> peticion : ruta.getValue()) {
				mvc.perform(peticion.get()).andExpect(status().isUnauthorized());
			}
		}
	}
}
