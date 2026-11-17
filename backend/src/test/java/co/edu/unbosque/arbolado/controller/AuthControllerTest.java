package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de inicio y cierre de sesión y del acceso por rol, con la cadena de seguridad real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

	/** Clave común de las cuentas de prueba. */
	private static final String CLAVE = "ClaveSegura1";

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mvc;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Repositorio de auditoría. */
	@Autowired
	private AuditoriaRepository auditoria;

	/**
	 * Crea una cuenta de prueba con la clave común.
	 *
	 * @param correo correo único de la cuenta
	 * @param rol    rol de la cuenta
	 * @param estado estado de la cuenta
	 */
	private void crearCuenta(String correo, Rol rol, EstadoCuenta estado) {
		String documento = String.format("%010d", Math.abs(correo.hashCode()) % 1_000_000_000L + 100);
		usuarios.save(new Usuario(documento, "Cuenta de prueba", correo, codificador.encode(CLAVE), rol, estado));
	}

	/**
	 * Inicia sesión y devuelve el token.
	 *
	 * @param correo correo de la cuenta
	 * @return el JWT recibido
	 * @throws Exception si la petición falla
	 */
	private String token(String correo) throws Exception {
		String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json(correo, CLAVE)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(cuerpo, "$.token");
	}

	/**
	 * Arma el cuerpo JSON de un inicio de sesión.
	 *
	 * @param correo     correo
	 * @param contrasena contraseña
	 * @return el JSON
	 */
	private static String json(String correo, String contrasena) {
		return "{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}";
	}

	/** El login correcto devuelve el token, su vencimiento y los datos de la cuenta sin la contraseña. */
	@Test
	void unLoginCorrectoDevuelveElToken() throws Exception {
		crearCuenta("login.ok@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("Login.OK@correo.com", CLAVE)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.expiraEn").isNotEmpty())
				.andExpect(jsonPath("$.usuario.correo").value("login.ok@correo.com"))
				.andExpect(jsonPath("$.usuario.rol").value("ESTUDIANTE"))
				.andExpect(jsonPath("$.usuario.contrasena").doesNotExist());
	}

	/** El administrador precargado inicia sesión con las credenciales de la configuración. */
	@Test
	void elAdministradorPrecargadoPuedeIniciarSesion() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("admin.pruebas@example.com", "clave-de-pruebas-1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"));
	}

	/** Contraseña errónea y correo inexistente dan la misma respuesta 401. */
	@Test
	void credencialesIncorrectasDanLaMismaRespuesta() throws Exception {
		crearCuenta("clave.mala@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);

		String conClaveMala = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("clave.mala@correo.com", "otra-clave")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
				.andReturn().getResponse().getContentAsString();
		String sinCuenta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("nadie@correo.com", "otra-clave")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
				.andReturn().getResponse().getContentAsString();

		assertThat((String) JsonPath.read(conClaveMala, "$.mensaje"))
				.isEqualTo(JsonPath.read(sinCuenta, "$.mensaje"));
	}

	/** Una cuenta pendiente con la clave correcta recibe 403; con clave errónea, 401 (no se revela el estado). */
	@Test
	void unaCuentaPendienteRecibe403SoloConLaClaveCorrecta() throws Exception {
		crearCuenta("pendiente@correo.com", Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE);

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("pendiente@correo.com", CLAVE)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CUENTA_PENDIENTE"));
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("pendiente@correo.com", "otra-clave")))
				.andExpect(status().isUnauthorized());
	}

	/** Un cuerpo inválido responde 400 con el detalle por campo. */
	@Test
	void unCuerpoInvalidoResponde400() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"correo\":\"no-es-correo\",\"contrasena\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.detalles").isNotEmpty());
	}

	/** Un token de estudiante abre las rutas de estudiante y no las de otro rol. */
	@Test
	void elTokenDaAccesoSegunElRol() throws Exception {
		crearCuenta("rol.estudiante@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);
		crearCuenta("rol.profesor@correo.com", Rol.PROFESOR, EstadoCuenta.ACTIVO);

		mvc.perform(get("/api/asignaturas").header(HttpHeaders.AUTHORIZATION,
						"Bearer " + token("rol.estudiante@correo.com")))
				.andExpect(status().isOk());
		mvc.perform(get("/api/asignaturas").header(HttpHeaders.AUTHORIZATION,
						"Bearer " + token("rol.profesor@correo.com")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	/** Sin token o con un token inválido, una ruta protegida responde 401. */
	@Test
	void sinTokenOConTokenInvalidoResponde401() throws Exception {
		mvc.perform(get("/api/asignaturas")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/asignaturas").header(HttpHeaders.AUTHORIZATION, "Bearer basura"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
	}

	/** Un token inválido en una ruta pública no la bloquea. */
	@Test
	void unTokenInvalidoNoBloqueaElLogin() throws Exception {
		crearCuenta("publica@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);

		mvc.perform(post("/api/auth/login").header(HttpHeaders.AUTHORIZATION, "Bearer vencido-o-basura")
						.contentType(MediaType.APPLICATION_JSON).content(json("publica@correo.com", CLAVE)))
				.andExpect(status().isOk());
	}

	/** Tras el logout el mismo token deja de servir. */
	@Test
	void despuesDelLogoutElTokenSeRechaza() throws Exception {
		crearCuenta("logout@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);
		String token = token("logout@correo.com");

		mvc.perform(get("/api/asignaturas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/asignaturas").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	/** El logout exige un token. */
	@Test
	void elLogoutSinTokenResponde401() throws Exception {
		mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
	}

	/** Cerrar la sesión no invalida otras sesiones del mismo usuario. */
	@Test
	void elLogoutSoloRevocaElTokenActual() throws Exception {
		crearCuenta("dos.sesiones@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);
		String primero = token("dos.sesiones@correo.com");
		String segundo = token("dos.sesiones@correo.com");

		mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + primero))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/asignaturas").header(HttpHeaders.AUTHORIZATION, "Bearer " + segundo))
				.andExpect(status().isOk());
	}

	/** Cada intento de login deja rastro en la auditoría, también los fallidos. */
	@Test
	void elLoginQuedaAuditado() throws Exception {
		long antes = auditoria.count();
		crearCuenta("auditado@correo.com", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);

		token("auditado@correo.com");
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(json("auditado@correo.com", "otra-clave")))
				.andExpect(status().isUnauthorized());

		assertThat(auditoria.count()).isEqualTo(antes + 2);
	}
}
