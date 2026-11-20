package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la consulta de auditoría: solo el administrador, filtros, orden y paginación (RF-51, RN-30). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaControllerTest {

	/** Ruta del recurso. */
	private static final String RUTA = "/api/admin/auditoria";

	/** Contador de documentos únicos de las cuentas. */
	private static final AtomicLong DOCUMENTOS = new AtomicLong(3_500_000_000L);

	/** Hash de una clave cualquiera, calculado una sola vez porque BCrypt es lento. */
	private static String hashClave;

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mvc;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Emisor de tokens. */
	@Autowired
	private JwtService jwt;

	/** Acceso directo a la base para sembrar registros con fecha conocida. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Administrador que consulta. */
	private Usuario admin;

	/** Cuenta a la que pertenecen los registros sembrados; aísla la prueba de los demás registros. */
	private Usuario actor;

	/** Crea las cuentas y siembra cuatro registros del actor en fechas conocidas. */
	@BeforeEach
	void preparar() {
		admin = cuenta(Rol.ADMINISTRADOR);
		actor = cuenta(Rol.ESTUDIANTE);
		registro("INICIO_SESION", "EXITOSO", "2026-01-10T08:00:00Z");
		registro("INICIO_SESION", "FALLIDO", "2026-02-10T08:00:00Z");
		registro("INSCRIPCION", "EXITOSO", "2026-03-10T08:00:00Z");
		registro("INSCRIPCION", "FALLIDO", "2026-04-10T08:00:00Z");
	}

	/**
	 * Guarda una cuenta activa con documento y correo únicos.
	 *
	 * @param rol rol de la cuenta
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol) {
		synchronized (AuditoriaControllerTest.class) {
			if (hashClave == null) {
				hashClave = codificador.encode("ClaveSegura1");
			}
		}
		long n = DOCUMENTOS.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), "Cuenta " + n, "a" + n + "@example.com", hashClave, rol,
				EstadoCuenta.ACTIVO));
	}

	/**
	 * Inserta un registro del actor con fecha fija.
	 *
	 * @param accion    evento
	 * @param resultado resultado
	 * @param fecha     instante ISO
	 */
	private void registro(String accion, String resultado, String fecha) {
		jdbc.update("insert into auditoria (usuario_id, usuario_correo, accion, fecha_hora, entidad, id_recurso, resultado, "
						+ "informacion_adicional) values (?, ?, ?, ?, 'USUARIO', ?, ?, ?)", actor.getId(), actor.getCorreo(),
				accion, Timestamp.from(Instant.parse(fecha)), String.valueOf(actor.getId()), resultado, accion + " " + fecha);
	}

	/**
	 * Consulta con el token de una cuenta.
	 *
	 * @param quien  cuenta que consulta
	 * @param filtro parámetros de la URL, sin el {@code ?}
	 * @return la respuesta
	 * @throws Exception si falla la petición
	 */
	private ResultActions consultar(Usuario quien, String filtro) throws Exception {
		return mvc.perform(get(RUTA + "?" + filtro).header(HttpHeaders.AUTHORIZATION, bearer(quien)));
	}

	/**
	 * Encabezado de autorización de una cuenta.
	 *
	 * @param quien cuenta
	 * @return {@code Bearer <token>}
	 */
	private String bearer(Usuario quien) {
		return "Bearer " + jwt.emitir(quien).token();
	}

	/** Por usuario devuelve sus registros del más reciente al más antiguo, con todos los campos. */
	@Test
	void filtraPorUsuarioOrdenadoDelMasReciente() throws Exception {
		consultar(admin, "usuarioId=" + actor.getId()).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(4))
				.andExpect(jsonPath("$.contenido[*].fechaHora").value(contains("2026-04-10T08:00:00Z",
						"2026-03-10T08:00:00Z", "2026-02-10T08:00:00Z", "2026-01-10T08:00:00Z")))
				.andExpect(jsonPath("$.contenido[0].accion").value("INSCRIPCION"))
				.andExpect(jsonPath("$.contenido[0].resultado").value("FALLIDO"))
				.andExpect(jsonPath("$.contenido[0].usuarioCorreo").value(actor.getCorreo()))
				.andExpect(jsonPath("$.contenido[0].entidad").value("USUARIO"))
				.andExpect(jsonPath("$.contenido[0].informacionAdicional").value("INSCRIPCION 2026-04-10T08:00:00Z"));
	}

	/** Los filtros de acción y resultado se combinan. */
	@Test
	void combinaAccionYResultado() throws Exception {
		consultar(admin, "usuarioId=" + actor.getId() + "&accion=INICIO_SESION&resultado=FALLIDO")
				.andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.contenido[0].fechaHora").value("2026-02-10T08:00:00Z"));
		consultar(admin, "usuarioId=" + actor.getId() + "&resultado=EXITOSO")
				.andExpect(jsonPath("$.contenido[*].accion").value(contains("INSCRIPCION", "INICIO_SESION")));
	}

	/** El rango de fechas es inclusivo y acepta zona horaria. */
	@Test
	void filtraPorRangoDeFechas() throws Exception {
		consultar(admin, "usuarioId=" + actor.getId() + "&desde=2026-02-10T08:00:00Z&hasta=2026-03-10T08:00:00Z")
				.andExpect(jsonPath("$.totalElementos").value(2));
		consultar(admin, "usuarioId=" + actor.getId() + "&desde=2026-03-10T03:00:00-05:00")
				.andExpect(jsonPath("$.contenido[*].fechaHora").value(contains("2026-04-10T08:00:00Z",
						"2026-03-10T08:00:00Z")));
	}

	/** La paginación respeta el tamaño y ajusta los valores fuera de rango. */
	@Test
	void pagina() throws Exception {
		consultar(admin, "usuarioId=" + actor.getId() + "&tamano=3&pagina=1")
				.andExpect(jsonPath("$.contenido", hasSize(1)))
				.andExpect(jsonPath("$.contenido[0].fechaHora").value("2026-01-10T08:00:00Z"))
				.andExpect(jsonPath("$.totalPaginas").value(2));
		consultar(admin, "usuarioId=" + actor.getId() + "&tamano=500&pagina=-3")
				.andExpect(jsonPath("$.tamano").value(100))
				.andExpect(jsonPath("$.pagina").value(0));
	}

	/** Un filtro con valor inválido responde 400. */
	@Test
	void filtroInvalido() throws Exception {
		consultar(admin, "accion=BORRAR_TODO").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
		consultar(admin, "desde=ayer").andExpect(status().isBadRequest());
	}

	/** Solo el administrador consulta; profesor y estudiante reciben 403 y sin token 401. */
	@Test
	void soloElAdministrador() throws Exception {
		consultar(actor, "").andExpect(status().isForbidden());
		consultar(cuenta(Rol.PROFESOR), "").andExpect(status().isForbidden());
		mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
	}

	/** No existen rutas para modificar ni borrar la auditoría (RN-30). */
	@Test
	void noSePuedeModificarNiBorrar() throws Exception {
		mvc.perform(delete(RUTA + "/1").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().is4xxClientError());
		mvc.perform(put(RUTA + "/1").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().is4xxClientError());
		consultar(admin, "usuarioId=" + actor.getId()).andExpect(jsonPath("$.totalElementos").value(4));
	}
}
