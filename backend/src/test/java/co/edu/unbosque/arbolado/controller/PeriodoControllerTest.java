package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import co.edu.unbosque.arbolado.repository.PeriodoRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.JwtService;
import co.edu.unbosque.arbolado.service.PendientesDePeriodo;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de los periodos con la cadena de seguridad real y la consulta de notas pendientes simulada. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PeriodoControllerTest {

	/** Clave de las cuentas de prueba. */
	private static final String CLAVE = "ClaveSegura1";

	/** Contador de documentos únicos de las cuentas. */
	private static final AtomicLong DOCUMENTOS = new AtomicLong(7_000_000_000L);

	/** Contador de códigos de periodo únicos (3000-0, 3000-1, …). */
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

	/** Consulta simulada de inscripciones sin nota definitiva. */
	@MockitoBean
	private PendientesDePeriodo pendientes;

	/** Deja todos los periodos inactivos: el estado "activo" es global y lo comparten las pruebas. */
	@BeforeEach
	void sinPeriodoActivo() {
		jdbc.update("update periodo set activo = false, marca_activo = null");
	}

	/**
	 * Código de periodo único.
	 *
	 * @return código con formato AAAA-N
	 */
	private static String codigoNuevo() {
		int n = CODIGOS.getAndIncrement();
		return (3000 + n / 10) + "-" + (n % 10);
	}

	/**
	 * Guarda una cuenta con documento y correo únicos.
	 *
	 * @param rol rol de la cuenta
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol) {
		long n = DOCUMENTOS.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), "Cuenta " + rol, "p" + n + "@example.com",
				codificador.encode(CLAVE), rol, EstadoCuenta.ACTIVO));
	}

	/**
	 * Encabezado de autorización de una cuenta nueva del rol dado.
	 *
	 * @param rol rol de la cuenta
	 * @return valor del encabezado {@code Authorization}
	 */
	private String bearer(Rol rol) {
		return "Bearer " + jwt.emitir(cuenta(rol)).token();
	}

	/**
	 * Cuerpo JSON de un periodo.
	 *
	 * @param codigo código del periodo
	 * @return el JSON
	 */
	private static String cuerpo(String codigo) {
		return """
				{"codigo":"%s","fechaInicio":"2026-08-01","fechaFin":"2026-12-05"}
				""".formatted(codigo);
	}

	/**
	 * Guarda un periodo inactivo directamente.
	 *
	 * @return el periodo guardado
	 */
	private Periodo periodo() {
		return periodos.save(new Periodo(codigoNuevo(), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30)));
	}

	/** El administrador crea un periodo inactivo y la creación queda auditada. */
	@Test
	void creaUnPeriodoInactivo() throws Exception {
		String codigo = codigoNuevo();
		long antes = auditoria.count();

		mvc.perform(post("/api/periodos").header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(codigo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.codigo").value(codigo))
				.andExpect(jsonPath("$.fechaInicio").value("2026-08-01"))
				.andExpect(jsonPath("$.activo").value(false));

		assertThat(periodos.existsByCodigo(codigo)).isTrue();
		assertThat(auditoria.count()).isEqualTo(antes + 1);
	}

	/** Un código repetido responde 409 {@code PERIODO_DUPLICADO} y deja auditoría fallida. */
	@Test
	void unCodigoRepetidoResponde409() throws Exception {
		Periodo existente = periodo();
		long antes = auditoria.count();

		mvc.perform(post("/api/periodos").header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(existente.getCodigo())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PERIODO_DUPLICADO"));

		assertThat(auditoria.count()).isEqualTo(antes + 1);
	}

	/** Código mal formado, fechas ausentes o rango invertido responden 400. */
	@Test
	void losDatosInvalidosResponden400() throws Exception {
		String admin = bearer(Rol.ADMINISTRADOR);
		List<String> cuerpos = List.of(
				"{\"codigo\":\"2026-12\",\"fechaInicio\":\"2026-08-01\",\"fechaFin\":\"2026-12-05\"}",
				"{\"codigo\":\"2026-2\",\"fechaFin\":\"2026-12-05\"}",
				"{\"codigo\":\"2026-2\",\"fechaInicio\":\"2026-12-05\",\"fechaFin\":\"2026-08-01\"}",
				"{\"codigo\":\"2026-2\",\"fechaInicio\":\"2026-08-01\",\"fechaFin\":\"2026-08-01\"}");

		for (String cuerpo : cuerpos) {
			mvc.perform(post("/api/periodos").header(HttpHeaders.AUTHORIZATION, admin)
							.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		}
	}

	/** La modificación cambia código y fechas, no el estado activo, y se puede repetir el propio código. */
	@Test
	void modificaSinCambiarElEstadoActivo() throws Exception {
		Periodo p = periodo();
		p.activar();
		periodos.save(p);
		String admin = bearer(Rol.ADMINISTRADOR);
		String nuevo = codigoNuevo();

		mvc.perform(put("/api/periodos/{id}", p.getId()).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(nuevo)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value(nuevo))
				.andExpect(jsonPath("$.fechaFin").value("2026-12-05"))
				.andExpect(jsonPath("$.activo").value(true));
		mvc.perform(put("/api/periodos/{id}", p.getId()).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(nuevo)))
				.andExpect(status().isOk());
	}

	/** Modificar con el código de otro periodo responde 409; uno inexistente, 404. */
	@Test
	void modificarConCodigoAjenoOInexistente() throws Exception {
		Periodo uno = periodo();
		Periodo otro = periodo();
		String admin = bearer(Rol.ADMINISTRADOR);

		mvc.perform(put("/api/periodos/{id}", uno.getId()).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(otro.getCodigo())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("PERIODO_DUPLICADO"));
		mvc.perform(put("/api/periodos/{id}", 999_999_999).header(HttpHeaders.AUTHORIZATION, admin)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(codigoNuevo())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
	}

	/** No existe ninguna ruta para eliminar un periodo. */
	@Test
	void noSePuedeEliminarUnPeriodo() throws Exception {
		Periodo p = periodo();

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/periodos/{id}", p.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR)))
				.andExpect(status().isMethodNotAllowed());
		assertThat(periodos.existsById(p.getId())).isTrue();
	}

	/** Sin periodo activo, el primero se activa sin consultar notas pendientes. */
	@Test
	void activaElPrimerPeriodoSinRestriccion() throws Exception {
		Periodo p = periodo();

		mvc.perform(post("/api/periodos/{id}/activar", p.getId()).header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(true));

		verify(pendientes, never()).contarSinDefinitiva(anyLong());
		assertThat(periodos.findByActivoTrue()).get().extracting(Periodo::getId).isEqualTo(p.getId());
	}

	/** Activar otro periodo deja uno solo activo: el anterior se desactiva en la misma operación. */
	@Test
	void activarOtroDesactivaElAnterior() throws Exception {
		Periodo primero = periodo();
		Periodo segundo = periodo();
		String admin = bearer(Rol.ADMINISTRADOR);
		when(pendientes.contarSinDefinitiva(primero.getId())).thenReturn(0L);

		mvc.perform(post("/api/periodos/{id}/activar", primero.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk());
		mvc.perform(post("/api/periodos/{id}/activar", segundo.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(segundo.getId()))
				.andExpect(jsonPath("$.activo").value(true));

		assertThat(periodos.findById(primero.getId()).orElseThrow().isActivo()).isFalse();
		assertThat(jdbc.queryForObject("select count(*) from periodo where activo = true", Long.class)).isEqualTo(1L);
		mvc.perform(get("/api/periodos/activo").header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(jsonPath("$.id").value(segundo.getId()));
	}

	/** Con inscripciones sin nota definitiva en el periodo vigente, la activación se rechaza y queda auditada. */
	@Test
	void notasPendientesBloqueanLaActivacion() throws Exception {
		Periodo vigente = periodo();
		Periodo siguiente = periodo();
		String admin = bearer(Rol.ADMINISTRADOR);
		mvc.perform(post("/api/periodos/{id}/activar", vigente.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk());
		when(pendientes.contarSinDefinitiva(vigente.getId())).thenReturn(3L);
		long antes = auditoria.count();

		mvc.perform(post("/api/periodos/{id}/activar", siguiente.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("PERIODO_CON_NOTAS_PENDIENTES"));

		assertThat(periodos.findById(vigente.getId()).orElseThrow().isActivo()).isTrue();
		assertThat(periodos.findById(siguiente.getId()).orElseThrow().isActivo()).isFalse();
		assertThat(auditoria.count()).isEqualTo(antes + 1);
	}

	/** Activar el periodo que ya está activo responde 200 y no consulta notas. */
	@Test
	void activarElActivoNoCambiaNada() throws Exception {
		Periodo p = periodo();
		String admin = bearer(Rol.ADMINISTRADOR);
		mvc.perform(post("/api/periodos/{id}/activar", p.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk());

		mvc.perform(post("/api/periodos/{id}/activar", p.getId()).header(HttpHeaders.AUTHORIZATION, admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(true));
		verify(pendientes, never()).contarSinDefinitiva(anyLong());
	}

	/** Activar un periodo inexistente responde 404. */
	@Test
	void activarInexistenteResponde404() throws Exception {
		mvc.perform(post("/api/periodos/{id}/activar", 999_999_999).header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR)))
				.andExpect(status().isNotFound());
	}

	/** El listado va del código más reciente al más antiguo. */
	@Test
	void listaLosPeriodosOrdenados() throws Exception {
		Periodo a = periodo();
		Periodo b = periodo();

		mvc.perform(get("/api/periodos").header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id==" + b.getId() + ")]").exists())
				.andExpect(jsonPath("$[?(@.id==" + a.getId() + ")]").exists());
		List<String> codigos = periodos.findAllByOrderByCodigoDesc().stream().map(Periodo::getCodigo).toList();
		assertThat(codigos).isSortedAccordingTo(java.util.Comparator.reverseOrder());
	}

	/** Cualquier rol consulta el periodo activo; sin periodo activo responde 404. */
	@Test
	void todosConsultanElPeriodoActivo() throws Exception {
		mvc.perform(get("/api/periodos/activo").header(HttpHeaders.AUTHORIZATION, bearer(Rol.ESTUDIANTE)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));

		Periodo p = periodo();
		mvc.perform(post("/api/periodos/{id}/activar", p.getId()).header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR)))
				.andExpect(status().isOk());

		for (Rol rol : Rol.values()) {
			mvc.perform(get("/api/periodos/activo").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(p.getId()));
		}
		mvc.perform(get("/api/periodos/activo")).andExpect(status().isUnauthorized());
	}

	/** Estudiantes y profesores reciben 403 en la administración de periodos; sin token, 401. */
	@Test
	void soloElAdministradorAdministraPeriodos() throws Exception {
		Periodo p = periodo();
		List<Supplier<MockHttpServletRequestBuilder>> peticiones = List.of(
				() -> get("/api/periodos"),
				() -> post("/api/periodos").contentType(MediaType.APPLICATION_JSON).content(cuerpo("2999-9")),
				() -> put("/api/periodos/{id}", p.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpo("2999-9")),
				() -> post("/api/periodos/{id}/activar", p.getId()));

		for (Rol rol : List.of(Rol.ESTUDIANTE, Rol.PROFESOR)) {
			for (Supplier<MockHttpServletRequestBuilder> peticion : peticiones) {
				mvc.perform(peticion.get().header(HttpHeaders.AUTHORIZATION, bearer(rol)))
						.andExpect(status().isForbidden())
						.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
			}
		}
		for (Supplier<MockHttpServletRequestBuilder> peticion : peticiones) {
			mvc.perform(peticion.get()).andExpect(status().isUnauthorized());
		}
		assertThat(periodos.existsByCodigo("2999-9")).isFalse();
		assertThat(periodos.findById(p.getId()).orElseThrow().isActivo()).isFalse();
	}
}
