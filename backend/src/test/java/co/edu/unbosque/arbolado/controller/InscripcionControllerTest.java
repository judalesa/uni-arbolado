package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.DiaSemana;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Horario;
import co.edu.unbosque.arbolado.model.Inscripcion;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import co.edu.unbosque.arbolado.repository.GrupoRepository;
import co.edu.unbosque.arbolado.repository.InscripcionRepository;
import co.edu.unbosque.arbolado.repository.PeriodoRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la inscripción (RN-14 y RN-16 a RN-23) con la cadena de seguridad y la base reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InscripcionControllerTest {

	/** Clave de las cuentas de prueba. */
	private static final String CLAVE = "ClaveSegura1";

	/** Contador de documentos únicos de las cuentas. */
	private static final AtomicLong DOCUMENTOS = new AtomicLong(7_000_000_000L);

	/** Contador de códigos de periodo únicos (7000-0, 7000-1, …). */
	private static final AtomicInteger CODIGOS = new AtomicInteger();

	/** Hash de la clave, calculado una sola vez: BCrypt es lento y se necesitan decenas de cuentas. */
	private static String hashClave;

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

	/** Repositorio de grupos. */
	@Autowired
	private GrupoRepository grupos;

	/** Repositorio de inscripciones. */
	@Autowired
	private InscripcionRepository inscripciones;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Emisor de tokens. */
	@Autowired
	private JwtService jwt;

	/** Acceso directo a la base para fijar estados y periodos. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Periodo activo de la prueba. */
	private Periodo activo;

	/** Profesor responsable de los grupos de la prueba. */
	private Usuario profesor;

	/** Estudiante que se inscribe en la prueba. */
	private Usuario estudiante;

	/** Deja un único periodo activo, creado para esta prueba, y crea el profesor y el estudiante. */
	@BeforeEach
	void preparar() {
		jdbc.update("update periodo set activo = false, marca_activo = null");
		activo = periodo();
		jdbc.update("update periodo set activo = true, marca_activo = true where id = ?", activo.getId());
		profesor = cuenta(Rol.PROFESOR);
		estudiante = cuenta(Rol.ESTUDIANTE);
	}

	/**
	 * Guarda una cuenta activa con documento y correo únicos.
	 *
	 * @param rol rol de la cuenta
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol) {
		synchronized (InscripcionControllerTest.class) {
			if (hashClave == null) {
				hashClave = codificador.encode(CLAVE);
			}
		}
		long n = DOCUMENTOS.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), "Cuenta " + rol, "i" + n + "@example.com", hashClave, rol,
				EstadoCuenta.ACTIVO));
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
		return periodos.save(new Periodo((7000 + n / 10) + "-" + (n % 10), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30)));
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
	 * Guarda un grupo del periodo activo con una franja.
	 *
	 * @param codigo asignatura
	 * @param dia    día de la franja
	 * @param inicio hora de inicio
	 * @param fin    hora de fin
	 * @return el grupo
	 */
	private Grupo grupo(String codigo, DiaSemana dia, int inicio, int fin) {
		return grupoEn(activo, codigo, dia, inicio, fin);
	}

	/**
	 * Guarda un grupo de un periodo cualquiera con una franja.
	 *
	 * @param periodo periodo del grupo
	 * @param codigo  asignatura
	 * @param dia     día de la franja
	 * @param inicio  hora de inicio
	 * @param fin     hora de fin
	 * @return el grupo
	 */
	private Grupo grupoEn(Periodo periodo, String codigo, DiaSemana dia, int inicio, int fin) {
		Asignatura asignatura = asignatura(codigo);
		Grupo grupo = new Grupo(grupos.siguienteNumero(asignatura.getId(), periodo.getId()), asignatura, periodo, profesor,
				"B", "301");
		grupo.reemplazarHorarios(List.of(new Horario(grupo, dia, LocalTime.of(inicio, 0), LocalTime.of(fin, 0))));
		return grupos.save(grupo);
	}

	/**
	 * Deja al estudiante cursando un grupo, sin pasar por las validaciones.
	 *
	 * @param quien estudiante
	 * @param grupo grupo
	 * @return la inscripción guardada
	 */
	private Inscripcion cursando(Usuario quien, Grupo grupo) {
		return inscripciones.save(new Inscripcion(quien, grupo));
	}

	/**
	 * Cambia el estado de una inscripción como lo haría el cierre de notas, con la marca vigente coherente.
	 *
	 * @param inscripcion inscripción a cerrar
	 * @param estado      {@code APROBADA} o {@code REPROBADA}
	 */
	private void cerrar(Inscripcion inscripcion, String estado) {
		jdbc.update("update inscripcion set estado = ?, marca_vigente = ? where id = ?", estado,
				"APROBADA".equals(estado) ? Boolean.TRUE : null, inscripcion.getId());
	}

	/**
	 * Deja a un estudiante con una asignatura aprobada en un periodo anterior.
	 *
	 * @param quien  estudiante
	 * @param codigo asignatura aprobada
	 */
	private void aprobada(Usuario quien, String codigo) {
		Grupo previo = grupoEn(periodo(), codigo, DiaSemana.SABADO, 6, 7);
		cerrar(cursando(quien, previo), "APROBADA");
	}

	/**
	 * Llena un grupo con estudiantes cursando.
	 *
	 * @param grupo    grupo a llenar
	 * @param cantidad estudiantes a inscribir
	 */
	private void llenar(Grupo grupo, int cantidad) {
		for (int i = 0; i < cantidad; i++) {
			cursando(cuenta(Rol.ESTUDIANTE), grupo);
		}
	}

	/**
	 * Pide la inscripción de un estudiante en un grupo.
	 *
	 * @param quien estudiante que actúa
	 * @param grupo grupo elegido
	 * @return la respuesta
	 * @throws Exception si falla la petición
	 */
	private ResultActions inscribir(Usuario quien, Grupo grupo) throws Exception {
		return mvc.perform(post("/api/inscripciones").header(HttpHeaders.AUTHORIZATION, bearer(quien))
				.contentType(MediaType.APPLICATION_JSON).content("{\"grupoId\":" + grupo.getId() + "}"));
	}

	/**
	 * Cuenta las inscripciones de un estudiante.
	 *
	 * @param quien estudiante
	 * @return cantidad
	 */
	private long cuantas(Usuario quien) {
		return inscripciones.count() == 0 ? 0 : jdbc.queryForObject(
				"select count(*) from inscripcion where estudiante_id = ?", Long.class, quien.getId());
	}

	/**
	 * Cuenta los registros de auditoría de inscripción de un usuario.
	 *
	 * @param quien     usuario
	 * @param resultado {@code EXITOSO} o {@code FALLIDO}
	 * @return cantidad
	 */
	private long auditorias(Usuario quien, String resultado) {
		return jdbc.queryForObject("select count(*) from auditoria where accion = 'INSCRIPCION' and usuario_id = ? and resultado = ?",
				Long.class, quien.getId(), resultado);
	}

	/** Una inscripción válida queda cursando, ocupa un cupo y se audita como exitosa. */
	@Test
	void inscribeYOcupaUnCupo() throws Exception {
		Grupo grupo = grupo("DSI101", DiaSemana.LUNES, 8, 10);

		inscribir(estudiante, grupo).andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("CURSANDO"))
				.andExpect(jsonPath("$.estudiante.id").value(estudiante.getId()))
				.andExpect(jsonPath("$.grupo.inscritos").value(1))
				.andExpect(jsonPath("$.grupo.cuposDisponibles").value(29))
				.andExpect(jsonPath("$.notaDefinitiva").doesNotExist());

		assertThat(cuantas(estudiante)).isEqualTo(1);
		assertThat(auditorias(estudiante, "EXITOSO")).isEqualTo(1);
	}

	/** La oferta del periodo activo refleja el cupo ocupado. */
	@Test
	void laOfertaMuestraElCupoOcupado() throws Exception {
		Grupo grupo = grupo("DSI101", DiaSemana.LUNES, 8, 10);
		inscribir(estudiante, grupo).andExpect(status().isCreated());

		mvc.perform(get("/api/grupos/oferta").header(HttpHeaders.AUTHORIZATION, bearer(estudiante)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + grupo.getId() + ")].cuposDisponibles").value(29));
	}

	/** Sin token no se puede inscribir; un profesor o un administrador tampoco. */
	@Test
	void soloElEstudianteInscribe() throws Exception {
		Grupo grupo = grupo("DSI101", DiaSemana.LUNES, 8, 10);
		String cuerpo = "{\"grupoId\":" + grupo.getId() + "}";

		mvc.perform(post("/api/inscripciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isUnauthorized());
		inscribir(profesor, grupo).andExpect(status().isForbidden());
		inscribir(cuenta(Rol.ADMINISTRADOR), grupo).andExpect(status().isForbidden());
		assertThat(inscripciones.countByGrupoId(grupo.getId())).isZero();
	}

	/** Sin grupo en el cuerpo es 400 y un grupo inexistente es 404. */
	@Test
	void validaElCuerpoYElGrupo() throws Exception {
		mvc.perform(post("/api/inscripciones").header(HttpHeaders.AUTHORIZATION, bearer(estudiante))
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/inscripciones").header(HttpHeaders.AUTHORIZATION, bearer(estudiante))
				.contentType(MediaType.APPLICATION_JSON).content("{\"grupoId\":999999999}"))
				.andExpect(status().isNotFound());
	}

	/** Un grupo de un periodo que no es el activo se rechaza (RN-14). */
	@Test
	void rechazaUnGrupoDeUnPeriodoNoActivo() throws Exception {
		Grupo futuro = grupoEn(periodo(), "DSI101", DiaSemana.LUNES, 8, 10);

		inscribir(estudiante, futuro).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos[0].codigo").value("PERIODO_NO_ACTIVO"));
		assertThat(cuantas(estudiante)).isZero();
	}

	/** Falta aprobar el prerrequisito: cursarlo no cuenta (RN-16). */
	@Test
	void exigeElPrerrequisitoAprobado() throws Exception {
		cursando(estudiante, grupo("MAT101", DiaSemana.MARTES, 8, 10));
		Grupo mat201 = grupo("MAT201", DiaSemana.LUNES, 8, 10);

		inscribir(estudiante, mat201).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos[0].codigo").value("PREREQUISITO_NO_CUMPLIDO"))
				.andExpect(jsonPath("$.motivos.length()").value(1));
	}

	/** Con el prerrequisito aprobado se puede inscribir. */
	@Test
	void permiteConElPrerrequisitoAprobado() throws Exception {
		aprobada(estudiante, "MAT101");

		inscribir(estudiante, grupo("MAT201", DiaSemana.LUNES, 8, 10)).andExpect(status().isCreated());
	}

	/** El grupo con 30 cursando está lleno, y el cupo 30 se puede ocupar (RN-11, RN-18). */
	@Test
	void respetaElCupoDe30() throws Exception {
		Grupo grupo = grupo("DSI101", DiaSemana.LUNES, 8, 10);
		llenar(grupo, 29);

		inscribir(estudiante, grupo).andExpect(status().isCreated())
				.andExpect(jsonPath("$.grupo.cuposDisponibles").value(0));
		inscribir(cuenta(Rol.ESTUDIANTE), grupo).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos[0].codigo").value("GRUPO_LLENO"));
	}

	/** Solo los estudiantes cursando ocupan cupo: quien ya terminó lo libera. */
	@Test
	void soloLosCursandoOcupanCupo() throws Exception {
		Grupo grupo = grupo("DSI101", DiaSemana.LUNES, 8, 10);
		llenar(grupo, 29);
		cerrar(cursando(cuenta(Rol.ESTUDIANTE), grupo), "REPROBADA");

		inscribir(estudiante, grupo).andExpect(status().isCreated());
	}

	/** Todos los incumplimientos se informan juntos, no solo el primero (RN-22). */
	@Test
	void informaTodosLosMotivosJuntos() throws Exception {
		Grupo lleno = grupo("MAT201", DiaSemana.LUNES, 8, 10);
		llenar(lleno, 30);

		inscribir(estudiante, lleno).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos.length()").value(2))
				.andExpect(jsonPath("$.motivos[*].codigo", containsInAnyOrder("GRUPO_LLENO", "PREREQUISITO_NO_CUMPLIDO")));
	}

	/** Una asignatura ya aprobada no se vuelve a inscribir (RN-19). */
	@Test
	void rechazaUnaAsignaturaYaAprobada() throws Exception {
		aprobada(estudiante, "DSI101");

		inscribir(estudiante, grupo("DSI101", DiaSemana.LUNES, 8, 10)).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos[0].codigo").value("ASIGNATURA_YA_APROBADA"));
	}

	/** La misma asignatura no se cursa en dos grupos a la vez (RN-19). */
	@Test
	void rechazaUnaAsignaturaYaCursando() throws Exception {
		cursando(estudiante, grupo("DSI101", DiaSemana.LUNES, 8, 10));

		inscribir(estudiante, grupo("DSI101", DiaSemana.MARTES, 8, 10)).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos[0].codigo").value("ASIGNATURA_YA_CURSANDO"));
	}

	/** Los créditos cursando más los nuevos no pueden pasar de 12 (RN-17). */
	@Test
	void rechazaSuperarLos12Creditos() throws Exception {
		aprobada(estudiante, "MAT101");
		cursando(estudiante, grupo("DSI101", DiaSemana.LUNES, 8, 10));
		cursando(estudiante, grupo("ING101", DiaSemana.MARTES, 8, 10));
		cursando(estudiante, grupo("PSI101", DiaSemana.MIERCOLES, 8, 10));
		cursando(estudiante, grupo("FIS201", DiaSemana.JUEVES, 8, 10));

		inscribir(estudiante, grupo("ALG201", DiaSemana.VIERNES, 8, 10)).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos.length()").value(1))
				.andExpect(jsonPath("$.motivos[0].codigo").value("CREDITOS_EXCEDIDOS"));
	}

	/** Llegar justo a 12 créditos está permitido. */
	@Test
	void permiteLlegarExactamenteA12Creditos() throws Exception {
		aprobada(estudiante, "MAT101");
		cursando(estudiante, grupo("DSI101", DiaSemana.LUNES, 8, 10));
		cursando(estudiante, grupo("ING101", DiaSemana.MARTES, 8, 10));
		cursando(estudiante, grupo("PSI101", DiaSemana.MIERCOLES, 8, 10));

		inscribir(estudiante, grupo("MAT201", DiaSemana.JUEVES, 8, 10)).andExpect(status().isCreated());
	}

	/** Un horario que se cruza con el de otra asignatura cursando se rechaza (RN-21). */
	@Test
	void rechazaUnHorarioTraslapado() throws Exception {
		cursando(estudiante, grupo("MAT101", DiaSemana.LUNES, 8, 10));

		inscribir(estudiante, grupo("ING101", DiaSemana.LUNES, 9, 11)).andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.motivos[0].codigo").value("HORARIO_TRASLAPADO"));
	}

	/** Empezar justo cuando otra clase termina, o ir otro día, no es traslape. */
	@Test
	void permiteHorariosContiguosYDeOtroDia() throws Exception {
		cursando(estudiante, grupo("MAT101", DiaSemana.LUNES, 8, 10));

		inscribir(estudiante, grupo("ING101", DiaSemana.LUNES, 10, 12)).andExpect(status().isCreated());
		inscribir(estudiante, grupo("DSI101", DiaSemana.MARTES, 8, 10)).andExpect(status().isCreated());
	}

	/** Una asignatura reprobada se puede reinscribir en un periodo posterior (RN-20). */
	@Test
	void permiteReinscribirUnaReprobada() throws Exception {
		Grupo anterior = grupoEn(periodo(), "DSI101", DiaSemana.SABADO, 6, 7);
		cerrar(cursando(estudiante, anterior), "REPROBADA");

		inscribir(estudiante, grupo("DSI101", DiaSemana.LUNES, 8, 10)).andExpect(status().isCreated());
		assertThat(cuantas(estudiante)).isEqualTo(2);
	}

	/** Si falla algo no se inscribe nada, y el rechazo queda auditado como fallido (RN-22, RN-30). */
	@Test
	void todoONadaYAuditaElRechazo() throws Exception {
		Grupo lleno = grupo("MAT201", DiaSemana.LUNES, 8, 10);
		llenar(lleno, 30);

		inscribir(estudiante, lleno).andExpect(status().isUnprocessableEntity());

		assertThat(cuantas(estudiante)).isZero();
		assertThat(auditorias(estudiante, "FALLIDO")).isEqualTo(1);
		assertThat(auditorias(estudiante, "EXITOSO")).isZero();
	}

	/** Con inscripciones pendientes no se puede activar otro periodo (A-02): el puerto ya cuenta de verdad. */
	@Test
	void conInscripcionesPendientesNoSeActivaOtroPeriodo() throws Exception {
		inscribir(estudiante, grupo("DSI101", DiaSemana.LUNES, 8, 10)).andExpect(status().isCreated());
		Periodo siguiente = periodo();

		mvc.perform(post("/api/periodos/" + siguiente.getId() + "/activar")
						.header(HttpHeaders.AUTHORIZATION, bearer(cuenta(Rol.ADMINISTRADOR))))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("PERIODO_CON_NOTAS_PENDIENTES"));
	}

	/** Seis estudiantes se disputan los 2 últimos cupos a la vez: entran exactamente 2, nunca 31. */
	@Test
	void elBloqueoEvitaPasarDe30() throws Exception {
		Grupo grupo = grupo("DSI101", DiaSemana.LUNES, 8, 10);
		llenar(grupo, 28);
		List<Usuario> aspirantes = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			aspirantes.add(cuenta(Rol.ESTUDIANTE));
		}

		ExecutorService hilos = Executors.newFixedThreadPool(aspirantes.size());
		try {
			List<Future<Integer>> resultados = new ArrayList<>();
			for (Usuario aspirante : aspirantes) {
				Callable<Integer> tarea = () -> inscribir(aspirante, grupo).andReturn().getResponse().getStatus();
				resultados.add(hilos.submit(tarea));
			}
			int creadas = 0;
			int rechazadas = 0;
			for (Future<Integer> resultado : resultados) {
				int codigo = resultado.get();
				creadas += codigo == 201 ? 1 : 0;
				rechazadas += codigo == 422 ? 1 : 0;
			}
			assertThat(creadas).isEqualTo(2);
			assertThat(rechazadas).isEqualTo(4);
		} finally {
			hilos.shutdownNow();
		}
		assertThat(inscripciones.countByGrupoId(grupo.getId())).isEqualTo(30);
	}
}
