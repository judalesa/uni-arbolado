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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de las consultas del estudiante y del profesor: cada rol solo ve lo suyo (RF-35 a RF-39, RF-44, RF-45). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultaControllerTest {

	/** Contador de documentos únicos de las cuentas. */
	private static final AtomicLong DOCUMENTOS = new AtomicLong(6_500_000_000L);

	/** Contador de códigos de periodo únicos (6000-0, 6000-1, …). */
	private static final AtomicInteger CODIGOS = new AtomicInteger();

	/** Hash de una clave cualquiera, calculado una sola vez porque BCrypt es lento. */
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

	/** Acceso directo a la base para fijar estados, notas y periodos. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Periodo activo de la prueba. */
	private Periodo activo;

	/** Profesor de los grupos de la prueba. */
	private Usuario profesor;

	/** Otro profesor, sin relación con los estudiantes de la prueba. */
	private Usuario otroProfesor;

	/** Estudiante que consulta. */
	private Usuario estudiante;

	/** Otro estudiante, cuyos datos no deben aparecer. */
	private Usuario otroEstudiante;

	/** Deja un único periodo activo, creado para esta prueba, y crea las cuentas. */
	@BeforeEach
	void preparar() {
		jdbc.update("update periodo set activo = false, marca_activo = null");
		activo = periodo();
		jdbc.update("update periodo set activo = true, marca_activo = true where id = ?", activo.getId());
		profesor = cuenta(Rol.PROFESOR);
		otroProfesor = cuenta(Rol.PROFESOR);
		estudiante = cuenta(Rol.ESTUDIANTE);
		otroEstudiante = cuenta(Rol.ESTUDIANTE);
	}

	/**
	 * Guarda una cuenta activa con documento y correo únicos.
	 *
	 * @param rol rol de la cuenta
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol) {
		synchronized (ConsultaControllerTest.class) {
			if (hashClave == null) {
				hashClave = codificador.encode("ClaveSegura1");
			}
		}
		long n = DOCUMENTOS.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), "Cuenta " + n, "c" + n + "@example.com", hashClave, rol,
				EstadoCuenta.ACTIVO));
	}

	/**
	 * Guarda un periodo inactivo con código único.
	 *
	 * @return el periodo
	 */
	private Periodo periodo() {
		int n = CODIGOS.getAndIncrement();
		return periodos.save(new Periodo((6000 + n / 10) + "-" + (n % 10), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30)));
	}

	/**
	 * Guarda un grupo con una franja.
	 *
	 * @param periodo     periodo del grupo
	 * @param responsable profesor del grupo
	 * @param codigo      asignatura
	 * @param dia         día de la franja
	 * @param inicio      hora de inicio
	 * @return el grupo
	 */
	private Grupo grupo(Periodo periodo, Usuario responsable, String codigo, DiaSemana dia, int inicio) {
		Asignatura asignatura = asignaturas.findByCodigo(codigo).orElseThrow();
		Grupo grupo = new Grupo(grupos.siguienteNumero(asignatura.getId(), periodo.getId()), asignatura, periodo,
				responsable, "B", "301");
		grupo.reemplazarHorarios(List.of(new Horario(grupo, dia, LocalTime.of(inicio, 0), LocalTime.of(inicio + 2, 0))));
		return grupos.save(grupo);
	}

	/**
	 * Inscribe sin pasar por las validaciones.
	 *
	 * @param quien estudiante
	 * @param grupo grupo
	 * @return la inscripción, cursando
	 */
	private Inscripcion cursando(Usuario quien, Grupo grupo) {
		return inscripciones.save(new Inscripcion(quien, grupo));
	}

	/**
	 * Cierra una inscripción con su definitiva, como lo haría el registro de notas.
	 *
	 * @param inscripcion inscripción
	 * @param estado      {@code APROBADA} o {@code REPROBADA}
	 * @param definitiva  nota definitiva
	 */
	private void cerrar(Inscripcion inscripcion, String estado, double definitiva) {
		jdbc.update("update inscripcion set estado = ?, marca_vigente = ?, nota_definitiva = ? where id = ?", estado,
				"APROBADA".equals(estado) ? Boolean.TRUE : null, definitiva, inscripcion.getId());
	}

	/**
	 * Hace un GET con el token de una cuenta.
	 *
	 * @param url   ruta
	 * @param quien cuenta que consulta
	 * @return la respuesta
	 * @throws Exception si falla la petición
	 */
	private ResultActions consultar(String url, Usuario quien) throws Exception {
		return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.emitir(quien).token()));
	}

	/** Sin periodo, el estudiante ve solo sus inscripciones del periodo activo. */
	@Test
	void misInscripcionesSonLasDelPeriodoActivo() throws Exception {
		Grupo dsi = grupo(activo, profesor, "DSI101", DiaSemana.LUNES, 8);
		cursando(estudiante, dsi);
		cursando(otroEstudiante, dsi);
		cursando(estudiante, grupo(periodo(), profesor, "MAT101", DiaSemana.MARTES, 8));

		consultar("/api/estudiante/inscripciones", estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].grupo.asignatura.codigo").value("DSI101"))
				.andExpect(jsonPath("$[0].estudiante.id").value(estudiante.getId()))
				.andExpect(jsonPath("$[0].grupo.inscritos").value(2));
	}

	/** Con un periodo, devuelve las de ese periodo con sus notas. */
	@Test
	void misInscripcionesDeOtroPeriodoConNotas() throws Exception {
		Periodo anterior = periodo();
		cerrar(cursando(estudiante, grupo(anterior, profesor, "MAT101", DiaSemana.MARTES, 8)), "APROBADA", 4.2);

		consultar("/api/estudiante/inscripciones?periodoId=" + anterior.getId(), estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].estado").value("APROBADA"))
				.andExpect(jsonPath("$[0].notaDefinitiva").value(4.2));
	}

	/** Sin periodo activo, la lista sale vacía en vez de fallar. */
	@Test
	void sinPeriodoActivoNoHayInscripciones() throws Exception {
		jdbc.update("update periodo set activo = false, marca_activo = null");

		consultar("/api/estudiante/inscripciones", estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	/** Cada asignatura del plan muestra su estado; aprobada manda sobre un intento reprobado anterior. */
	@Test
	void planConEstadoDeCadaAsignatura() throws Exception {
		Periodo anterior = periodo();
		cerrar(cursando(estudiante, grupo(anterior, profesor, "MAT101", DiaSemana.LUNES, 8)), "REPROBADA", 2.1);
		cerrar(cursando(estudiante, grupo(periodo(), profesor, "MAT101", DiaSemana.LUNES, 8)), "APROBADA", 3.5);
		cerrar(cursando(estudiante, grupo(anterior, profesor, "MAT201", DiaSemana.MARTES, 8)), "REPROBADA", 1.8);
		cursando(estudiante, grupo(activo, profesor, "DSI101", DiaSemana.MIERCOLES, 8));

		String plan = "/api/estudiante/plan-estado";
		consultar(plan, estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value((int) asignaturas.count()))
				.andExpect(jsonPath("$[?(@.asignatura.codigo == 'MAT101')].estado").value(contains("APROBADA")))
				.andExpect(jsonPath("$[?(@.asignatura.codigo == 'MAT101')].notaDefinitiva").value(contains(3.5)))
				.andExpect(jsonPath("$[?(@.asignatura.codigo == 'MAT201')].estado").value(contains("REPROBADA")))
				.andExpect(jsonPath("$[?(@.asignatura.codigo == 'DSI101')].estado").value(contains("CURSANDO")));
		consultar(plan, otroEstudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.asignatura.codigo == 'MAT101')].estado").value(contains("POR_CURSAR")));
	}

	/** Una asignatura reprobada que se vuelve a cursar figura como cursando. */
	@Test
	void reprobadaQueSeRecursaFiguraCursando() throws Exception {
		cerrar(cursando(estudiante, grupo(periodo(), profesor, "MAT101", DiaSemana.LUNES, 8)), "REPROBADA", 2.0);
		cursando(estudiante, grupo(activo, profesor, "MAT101", DiaSemana.LUNES, 8));

		consultar("/api/estudiante/plan-estado", estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.asignatura.codigo == 'MAT101')].estado").value(contains("CURSANDO")));
	}

	/** El horario trae una fila por franja de lo que cursa, ordenada por día, sin lo cerrado ni lo ajeno. */
	@Test
	void horarioDelEstudiante() throws Exception {
		cursando(estudiante, grupo(activo, profesor, "MAT101", DiaSemana.MIERCOLES, 10));
		cursando(estudiante, grupo(activo, profesor, "DSI101", DiaSemana.LUNES, 8));
		cerrar(cursando(estudiante, grupo(periodo(), profesor, "MAT201", DiaSemana.MARTES, 8)), "REPROBADA", 2.0);
		cursando(otroEstudiante, grupo(activo, profesor, "MAT201", DiaSemana.JUEVES, 8));

		consultar("/api/estudiante/horario", estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].diaSemana").value("LUNES"))
				.andExpect(jsonPath("$[0].asignatura.codigo").value("DSI101"))
				.andExpect(jsonPath("$[0].horaInicio").value("08:00"))
				.andExpect(jsonPath("$[0].salon").value("301"))
				.andExpect(jsonPath("$[1].diaSemana").value("MIERCOLES"));
	}

	/** El historial agrupa todos los intentos por periodo, del código más alto al más bajo. */
	@Test
	void historialAgrupadoPorPeriodo() throws Exception {
		Periodo otro = periodo();
		cerrar(cursando(estudiante, grupo(otro, profesor, "MAT101", DiaSemana.LUNES, 8)), "REPROBADA", 2.0);
		cursando(estudiante, grupo(activo, profesor, "MAT101", DiaSemana.LUNES, 8));
		cursando(estudiante, grupo(activo, profesor, "DSI101", DiaSemana.MARTES, 8));
		cursando(otroEstudiante, grupo(otro, profesor, "DSI101", DiaSemana.MARTES, 8));

		consultar("/api/estudiante/historial", estudiante).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].periodo.codigo").value(otro.getCodigo()))
				.andExpect(jsonPath("$[0].inscripciones", hasSize(1)))
				.andExpect(jsonPath("$[0].inscripciones[0].estado").value("REPROBADA"))
				.andExpect(jsonPath("$[1].periodo.codigo").value(activo.getCodigo()))
				.andExpect(jsonPath("$[1].inscripciones", hasSize(2)));
	}

	/** Las consultas del estudiante exigen sesión y el rol de estudiante. */
	@Test
	void consultasDelEstudianteSoloParaEstudiantes() throws Exception {
		mvc.perform(get("/api/estudiante/historial")).andExpect(status().isUnauthorized());
		consultar("/api/estudiante/horario", profesor).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
	}

	/** El profesor ve los estudiantes de su grupo en cualquier estado, con sus notas. */
	@Test
	void estudiantesDeMiGrupo() throws Exception {
		Grupo grupo = grupo(activo, profesor, "DSI101", DiaSemana.LUNES, 8);
		cursando(estudiante, grupo);
		cerrar(cursando(otroEstudiante, grupo), "REPROBADA", 2.4);

		consultar("/api/profesor/grupos/" + grupo.getId() + "/estudiantes", profesor).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[?(@.estudiante.id == " + otroEstudiante.getId() + ")].notaDefinitiva")
						.value(contains(2.4)));
	}

	/** Un grupo de otro profesor responde 403 y uno inexistente 404. */
	@Test
	void estudiantesDeGrupoAjenoOInexistente() throws Exception {
		Grupo ajeno = grupo(activo, otroProfesor, "DSI101", DiaSemana.LUNES, 8);

		consultar("/api/profesor/grupos/" + ajeno.getId() + "/estudiantes", profesor).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		consultar("/api/profesor/grupos/999999/estudiantes", profesor).andExpect(status().isNotFound());
	}

	/** El horario del profesor trae solo sus grupos del periodo activo. */
	@Test
	void horarioDelProfesor() throws Exception {
		grupo(activo, profesor, "MAT101", DiaSemana.VIERNES, 14);
		grupo(activo, profesor, "DSI101", DiaSemana.MARTES, 8);
		grupo(periodo(), profesor, "MAT201", DiaSemana.LUNES, 8);
		grupo(activo, otroProfesor, "MAT201", DiaSemana.LUNES, 10);

		consultar("/api/profesor/horario", profesor).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].diaSemana").value("MARTES"))
				.andExpect(jsonPath("$[1].asignatura.codigo").value("MAT101"))
				.andExpect(jsonPath("$[1].horaFin").value("16:00"));
	}

	/** El profesor puede ver el historial completo de un estudiante de sus grupos. */
	@Test
	void historialDeUnEstudianteDeMiGrupo() throws Exception {
		cursando(estudiante, grupo(activo, profesor, "DSI101", DiaSemana.LUNES, 8));
		cerrar(cursando(estudiante, grupo(periodo(), otroProfesor, "MAT101", DiaSemana.LUNES, 8)), "APROBADA", 4.0);

		consultar("/api/profesor/estudiantes/" + estudiante.getId() + "/historial", profesor)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	/** Un estudiante que nunca estuvo en sus grupos da 403; un id que no es de estudiante, 404. */
	@Test
	void historialDeEstudianteAjenoOInexistente() throws Exception {
		cursando(otroEstudiante, grupo(activo, otroProfesor, "DSI101", DiaSemana.LUNES, 8));

		consultar("/api/profesor/estudiantes/" + otroEstudiante.getId() + "/historial", profesor)
				.andExpect(status().isForbidden());
		consultar("/api/profesor/estudiantes/" + otroProfesor.getId() + "/historial", profesor)
				.andExpect(status().isNotFound());
		consultar("/api/profesor/estudiantes/999999/historial", profesor).andExpect(status().isNotFound());
	}

	/** Las consultas del profesor no están abiertas a estudiantes ni administradores. */
	@Test
	void consultasDelProfesorSoloParaProfesores() throws Exception {
		consultar("/api/profesor/horario", estudiante).andExpect(status().isForbidden());
		consultar("/api/profesor/estudiantes/" + estudiante.getId() + "/historial", cuenta(Rol.ADMINISTRADOR))
				.andExpect(status().isForbidden());
	}
}
