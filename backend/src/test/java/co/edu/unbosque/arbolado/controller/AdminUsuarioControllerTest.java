package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la administración de usuarios y del perfil propio, con la cadena de seguridad y tokens reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUsuarioControllerTest {

	/** Clave de las cuentas de prueba. */
	private static final String CLAVE = "ClaveSegura1";

	/** Contador que da un documento y un correo únicos a cada cuenta creada. */
	private static final AtomicLong SECUENCIA = new AtomicLong(8_000_000_000L);

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mvc;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Repositorio de auditoría. */
	@Autowired
	private AuditoriaRepository auditoria;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Emisor de tokens, para autenticarse sin pasar por el login. */
	@Autowired
	private JwtService jwt;

	/**
	 * Guarda una cuenta con documento y correo únicos.
	 *
	 * @param rol    rol de la cuenta
	 * @param estado estado de la cuenta
	 * @param nombre nombre completo
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol, EstadoCuenta estado, String nombre) {
		long n = SECUENCIA.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), nombre, "u" + n + "@example.com",
				codificador.encode(CLAVE), rol, estado));
	}

	/**
	 * Encabezado de autorización con un token válido de la cuenta.
	 *
	 * @param usuario cuenta que actúa
	 * @return valor del encabezado {@code Authorization}
	 */
	private String bearer(Usuario usuario) {
		return "Bearer " + jwt.emitir(usuario).token();
	}

	/**
	 * Cuerpo JSON de la creación de un profesor.
	 *
	 * @param documento documento
	 * @param correo    correo
	 * @return el JSON
	 */
	private static String cuerpoProfesor(String documento, String correo) {
		return """
				{"documento":"%s","nombreCompleto":"Profesor Nuevo","correo":"%s","contrasena":"%s"}
				""".formatted(documento, correo, CLAVE);
	}

	/** El administrador crea un profesor activo, con rol correcto y sin exponer la contraseña. */
	@Test
	void elAdministradorCreaUnProfesorActivo() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		long n = SECUENCIA.incrementAndGet();
		long antes = auditoria.count();

		mvc.perform(post("/api/admin/profesores").header(HttpHeaders.AUTHORIZATION, bearer(admin))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpoProfesor(String.valueOf(n), "Prof" + n + "@Example.com")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rol").value("PROFESOR"))
				.andExpect(jsonPath("$.estado").value("ACTIVO"))
				.andExpect(jsonPath("$.correo").value("prof" + n + "@example.com"))
				.andExpect(jsonPath("$.contrasena").doesNotExist());

		Usuario guardado = usuarios.findByDocumento(String.valueOf(n)).orElseThrow();
		assertThat(guardado.getContrasena()).isNotEqualTo(CLAVE);
		assertThat(codificador.matches(CLAVE, guardado.getContrasena())).isTrue();
		assertThat(auditoria.count()).isEqualTo(antes + 1);
	}

	/** Un documento ya usado, incluso por una cuenta pendiente, responde 409 y deja auditoría fallida. */
	@Test
	void unDocumentoRepetidoResponde409() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		Usuario pendiente = cuenta(Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE, "Pendiente Prueba");
		long antes = auditoria.count();

		mvc.perform(post("/api/admin/profesores").header(HttpHeaders.AUTHORIZATION, bearer(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoProfesor(pendiente.getDocumento(), "otro" + SECUENCIA.incrementAndGet() + "@example.com")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DOCUMENTO_DUPLICADO"));

		assertThat(auditoria.count()).isEqualTo(antes + 1);
	}

	/** Un correo ya usado responde 409 {@code CORREO_DUPLICADO}, sin importar mayúsculas. */
	@Test
	void unCorreoRepetidoResponde409() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		Usuario existente = cuenta(Rol.ESTUDIANTE, EstadoCuenta.ACTIVO, "Existente Prueba");

		mvc.perform(post("/api/admin/profesores").header(HttpHeaders.AUTHORIZATION, bearer(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoProfesor(String.valueOf(SECUENCIA.incrementAndGet()), existente.getCorreo().toUpperCase())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
	}

	/** Un documento que no tiene 10 dígitos o una contraseña corta responden 400 con el campo señalado. */
	@Test
	void losDatosInvalidosResponden400() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");

		mvc.perform(post("/api/admin/profesores").header(HttpHeaders.AUTHORIZATION, bearer(admin))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"documento":"123","nombreCompleto":"Ab","correo":"no-es-correo","contrasena":"corta"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	/** Estudiantes y profesores reciben 403 en todas las rutas de administración; sin token, 401. */
	@Test
	void soloElAdministradorEntraALasRutasDeAdministracion() throws Exception {
		Usuario estudiante = cuenta(Rol.ESTUDIANTE, EstadoCuenta.ACTIVO, "Estudiante Prueba");
		Usuario profesor = cuenta(Rol.PROFESOR, EstadoCuenta.ACTIVO, "Profesor Prueba");
		// Proveedores y no builders: el builder es mutable y el encabezado de una vuelta pasaría a la siguiente.
		List<Supplier<MockHttpServletRequestBuilder>> peticiones = List.of(
				() -> post("/api/admin/profesores").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoProfesor("1234567890", "x@example.com")),
				() -> get("/api/admin/profesores"),
				() -> get("/api/admin/profesores/{id}", profesor.getId()),
				() -> get("/api/admin/estudiantes"),
				() -> get("/api/admin/estudiantes/{id}", estudiante.getId()));

		for (Usuario intruso : List.of(estudiante, profesor)) {
			for (Supplier<MockHttpServletRequestBuilder> peticion : peticiones) {
				mvc.perform(peticion.get().header(HttpHeaders.AUTHORIZATION, bearer(intruso)))
						.andExpect(status().isForbidden())
						.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
			}
		}
		for (Supplier<MockHttpServletRequestBuilder> peticion : peticiones) {
			mvc.perform(peticion.get())
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
		}
		assertThat(usuarios.existsByDocumento("1234567890")).isFalse();
	}

	/** El listado de profesores solo trae profesores y filtra por nombre, documento o correo. */
	@Test
	void listaYBuscaProfesores() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		Usuario ana = cuenta(Rol.PROFESOR, EstadoCuenta.ACTIVO, "Zzprofe Ana Lista");
		cuenta(Rol.PROFESOR, EstadoCuenta.ACTIVO, "Zzprofe Beto Lista");
		cuenta(Rol.ESTUDIANTE, EstadoCuenta.ACTIVO, "Zzprofe Estudiante Lista");

		mvc.perform(get("/api/admin/profesores").param("q", "  ZZPROFE ").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido", hasSize(2)))
				.andExpect(jsonPath("$.contenido[0].nombreCompleto").value("Zzprofe Ana Lista"))
				.andExpect(jsonPath("$.contenido[1].nombreCompleto").value("Zzprofe Beto Lista"))
				.andExpect(jsonPath("$.totalElementos").value(2));
		mvc.perform(get("/api/admin/profesores").param("q", ana.getDocumento()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.contenido", hasSize(1)))
				.andExpect(jsonPath("$.contenido[0].id").value(ana.getId()));
		mvc.perform(get("/api/admin/profesores").param("q", ana.getCorreo().toUpperCase()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.contenido", hasSize(1)));
	}

	/** El listado de estudiantes incluye a los pendientes y pagina con el tamaño pedido. */
	@Test
	void listaEstudiantesPaginados() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		cuenta(Rol.ESTUDIANTE, EstadoCuenta.ACTIVO, "Zzpagina A");
		cuenta(Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE, "Zzpagina B");
		cuenta(Rol.ESTUDIANTE, EstadoCuenta.ACTIVO, "Zzpagina C");

		mvc.perform(get("/api/admin/estudiantes").param("q", "zzpagina").param("tamano", "2")
						.header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido", hasSize(2)))
				.andExpect(jsonPath("$.totalElementos").value(3))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.tamano").value(2));
		mvc.perform(get("/api/admin/estudiantes").param("q", "zzpagina").param("tamano", "2").param("pagina", "1")
						.header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.contenido", hasSize(1)))
				.andExpect(jsonPath("$.contenido[0].estado").value("ACTIVO"))
				.andExpect(jsonPath("$.pagina").value(1));
	}

	/** Un tamaño fuera de rango se ajusta a 100 y una página negativa a 0, sin error. */
	@Test
	void ajustaLosParametrosDePaginacion() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");

		mvc.perform(get("/api/admin/estudiantes").param("tamano", "5000").param("pagina", "-3")
						.header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tamano").value(100))
				.andExpect(jsonPath("$.pagina").value(0));
	}

	/** Los comodines de LIKE se buscan como texto: "%" y "_" no actúan como patrón. */
	@Test
	void losComodinesSeBuscanComoTexto() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		cuenta(Rol.ESTUDIANTE, EstadoCuenta.ACTIVO, "Zzcomodin Normal");

		mvc.perform(get("/api/admin/estudiantes").param("q", "zzcomodin%").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.totalElementos").value(0));
		mvc.perform(get("/api/admin/estudiantes").param("q", "zzcomodin_normal").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.totalElementos").value(0));
		mvc.perform(get("/api/admin/estudiantes").param("q", "zzcomodin normal").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(jsonPath("$.totalElementos").value(1));
	}

	/** Cada rol se consulta solo por su propia ruta: el id de un estudiante no sirve en /profesores. */
	@Test
	void consultaPorIdRespetaElRol() throws Exception {
		Usuario admin = cuenta(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, "Admin Prueba");
		Usuario profesor = cuenta(Rol.PROFESOR, EstadoCuenta.ACTIVO, "Profesor Consulta");
		Usuario estudiante = cuenta(Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE, "Estudiante Consulta");
		String autorizacion = bearer(admin);

		mvc.perform(get("/api/admin/profesores/{id}", profesor.getId()).header(HttpHeaders.AUTHORIZATION, autorizacion))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documento").value(profesor.getDocumento()))
				.andExpect(jsonPath("$.contrasena").doesNotExist());
		mvc.perform(get("/api/admin/estudiantes/{id}", estudiante.getId()).header(HttpHeaders.AUTHORIZATION, autorizacion))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
		mvc.perform(get("/api/admin/profesores/{id}", estudiante.getId()).header(HttpHeaders.AUTHORIZATION, autorizacion))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
		mvc.perform(get("/api/admin/estudiantes/{id}", profesor.getId()).header(HttpHeaders.AUTHORIZATION, autorizacion))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/admin/estudiantes/{id}", 999_999_999).header(HttpHeaders.AUTHORIZATION, autorizacion))
				.andExpect(status().isNotFound());
	}

	/** Cualquier rol consulta su propio perfil, que sale del token. */
	@Test
	void cadaRolConsultaSuPerfil() throws Exception {
		for (Rol rol : Rol.values()) {
			Usuario usuario = cuenta(rol, EstadoCuenta.ACTIVO, "Perfil " + rol);

			mvc.perform(get("/api/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(usuario)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(usuario.getId()))
					.andExpect(jsonPath("$.rol").value(rol.name()))
					.andExpect(jsonPath("$.contrasena").doesNotExist());
		}
	}

	/** Sin token no hay perfil. */
	@Test
	void sinTokenElPerfilResponde401() throws Exception {
		mvc.perform(get("/api/usuarios/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
	}
}
