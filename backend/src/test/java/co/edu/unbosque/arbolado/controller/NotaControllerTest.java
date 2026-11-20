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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas del registro de notas: rango, inmutabilidad, permisos, definitiva y auditoría. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotaControllerTest {

	/** Contador de documentos únicos de las cuentas. */
	private static final AtomicLong DOCUMENTOS = new AtomicLong(4_500_000_000L);

	/** Contador de códigos de periodo únicos (3100-0, 3100-1, …). */
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

	/** Acceso directo a la base para fijar periodos y leer columnas. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Periodo activo de la prueba. */
	private Periodo activo;

	/** Profesor del grupo. */
	private Usuario profesor;

	/** Otro profesor, sin relación con el grupo. */
	private Usuario otroProfesor;

	/** Estudiante calificado. */
	private Usuario estudiante;

	/** Grupo del profesor en el periodo activo. */
	private Grupo grupo;

	/** Inscripción cursando del estudiante en el grupo. */
	private Inscripcion inscripcion;

	/** Deja un único periodo activo con un grupo y una inscripción cursando. */
	@BeforeEach
	void preparar() {
		jdbc.update("update periodo set activo = false, marca_activo = null");
		activo = periodo();
		jdbc.update("update periodo set activo = true, marca_activo = true where id = ?", activo.getId());
		profesor = cuenta(Rol.PROFESOR);
		otroProfesor = cuenta(Rol.PROFESOR);
		estudiante = cuenta(Rol.ESTUDIANTE);
		grupo = grupo(activo);
		inscripcion = inscripciones.save(new Inscripcion(estudiante, grupo));
	}

	/**
	 * Guarda una cuenta activa con documento y correo únicos.
	 *
	 * @param rol rol de la cuenta
	 * @return la cuenta guardada
	 */
	private Usuario cuenta(Rol rol) {
		synchronized (NotaControllerTest.class) {
			if (hashClave == null) {
				hashClave = codificador.encode("ClaveSegura1");
			}
		}
		long n = DOCUMENTOS.incrementAndGet();
		return usuarios.save(new Usuario(String.valueOf(n), "Cuenta " + n, "n" + n + "@example.com", hashClave, rol,
				EstadoCuenta.ACTIVO));
	}

	/**
	 * Guarda un periodo inactivo con código único.
	 *
	 * @return el periodo
	 */
	private Periodo periodo() {
		int n = CODIGOS.getAndIncrement();
		return periodos.save(new Periodo((3100 + n / 10) + "-" + (n % 10), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30)));
	}

	/**
	 * Guarda un grupo de DSI101 del profesor con una franja.
	 *
	 * @param periodo periodo del grupo
	 * @return el grupo
	 */
	private Grupo grupo(Periodo periodo) {
		Asignatura asignatura = asignaturas.findByCodigo("DSI101").orElseThrow();
		Grupo nuevo = new Grupo(grupos.siguienteNumero(asignatura.getId(), periodo.getId()), asignatura, periodo,
				profesor, "B", "301");
		nuevo.reemplazarHorarios(List.of(new Horario(nuevo, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0))));
		return grupos.save(nuevo);
	}

	/**
	 * Registra una nota con el token de una cuenta.
	 *
	 * @param quien  cuenta que registra
	 * @param id     inscripción
	 * @param cuerpo JSON de la petición
	 * @return la respuesta
	 * @throws Exception si falla la petición
	 */
	private ResultActions registrar(Usuario quien, Long id, String cuerpo) throws Exception {
		return mvc.perform(post("/api/profesor/inscripciones/" + id + "/notas")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.emitir(quien).token())
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	/**
	 * Registra la nota de un corte con el profesor del grupo.
	 *
	 * @param corte corte
	 * @param valor nota
	 * @return la respuesta
	 * @throws Exception si falla la petición
	 */
	private ResultActions nota(int corte, String valor) throws Exception {
		return registrar(profesor, inscripcion.getId(), "{\"corte\":" + corte + ",\"valor\":" + valor + "}");
	}

	/**
	 * Lee las columnas de estado de la inscripción directamente de la base.
	 *
	 * @return la fila
	 */
	private Map<String, Object> fila() {
		return jdbc.queryForMap("select estado, marca_vigente, nota_corte1, nota_definitiva from inscripcion where id = ?",
				inscripcion.getId());
	}

	/**
	 * Cuenta auditorías de registro de nota sobre la inscripción.
	 *
	 * @param resultado {@code EXITOSO} o {@code FALLIDO}
	 * @return cantidad
	 */
	private long auditorias(String resultado) {
		return jdbc.queryForObject("select count(*) from auditoria where accion = 'REGISTRO_NOTA' and id_recurso = ? "
				+ "and resultado = ?", Long.class, String.valueOf(inscripcion.getId()), resultado);
	}

	/** Un corte se registra y la inscripción sigue cursando, sin definitiva. */
	@Test
	void registraUnCorte() throws Exception {
		nota(1, "3.5").andExpect(status().isCreated())
				.andExpect(jsonPath("$.notaCorte1").value(3.5))
				.andExpect(jsonPath("$.notaCorte2").value(nullValue()))
				.andExpect(jsonPath("$.notaDefinitiva").value(nullValue()))
				.andExpect(jsonPath("$.estado").value("CURSANDO"));
		assertThat(auditorias("EXITOSO")).isEqualTo(1);
	}

	/** Con el tercer corte se aprueba sobre la nota redondeada (2.96 → 3.0) y se libera el cupo. */
	@Test
	void tercerCorteApruebaConRedondeo() throws Exception {
		nota(1, "3.0").andExpect(status().isCreated());
		nota(2, "3.0").andExpect(status().isCreated());
		nota(3, "2.9").andExpect(status().isCreated())
				.andExpect(jsonPath("$.notaDefinitiva").value(3.0))
				.andExpect(jsonPath("$.estado").value("APROBADA"))
				.andExpect(jsonPath("$.grupo.inscritos").value(0));
		assertThat(fila().get("marca_vigente")).isEqualTo(Boolean.TRUE);
		assertThat(jdbc.queryForObject("select informacion_adicional from auditoria where accion = 'REGISTRO_NOTA' "
				+ "and id_recurso = ? and informacion_adicional like '%Definitiva%'", String.class,
				String.valueOf(inscripcion.getId()))).contains("anterior vacío, nuevo 2.9", "Definitiva 3.0, APROBADA");
	}

	/** Con 2.94 se redondea a 2.9 y reprueba; la marca vigente queda nula para poder recursar. */
	@Test
	void tercerCorteReprueba() throws Exception {
		nota(1, "2.9");
		nota(2, "2.9");
		nota(3, "3.0").andExpect(status().isCreated())
				.andExpect(jsonPath("$.notaDefinitiva").value(2.9))
				.andExpect(jsonPath("$.estado").value("REPROBADA"));
		assertThat(fila().get("marca_vigente")).isNull();
		assertThat(fila().get("estado")).isEqualTo("REPROBADA");
	}

	/** Los cortes pueden llegar en cualquier orden; la definitiva sale cuando están los tres. */
	@Test
	void cortesEnCualquierOrden() throws Exception {
		nota(3, "4.0").andExpect(jsonPath("$.estado").value("CURSANDO"));
		nota(1, "2.0").andExpect(jsonPath("$.notaDefinitiva").value(nullValue()));
		nota(2, "2.5").andExpect(jsonPath("$.notaDefinitiva").value(3.0))
				.andExpect(jsonPath("$.estado").value("APROBADA"));
	}

	/** Un corte con nota no se vuelve a escribir: 409 y la nota original se conserva. */
	@Test
	void segundoRegistroDelMismoCorteRechazado() throws Exception {
		nota(1, "3.5").andExpect(status().isCreated());
		nota(1, "4.5").andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("NOTA_YA_REGISTRADA"));
		assertThat(fila().get("nota_corte1").toString()).isEqualTo("3.5");
		assertThat(auditorias("FALLIDO")).isEqualTo(1);
	}

	/** Una inscripción cerrada no admite más notas. */
	@Test
	void inscripcionCerradaNoAdmiteNotas() throws Exception {
		nota(1, "4.0");
		nota(2, "4.0");
		nota(3, "4.0");
		nota(3, "1.0").andExpect(status().isConflict());
		assertThat(fila().get("nota_definitiva").toString()).isEqualTo("4.0");
	}

	/** Fuera de rango, con dos decimales, corte inválido o sin datos: 400 y nada cambia. */
	@Test
	void validaRangoYDecimal() throws Exception {
		nota(1, "5.1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"));
		nota(1, "-0.1").andExpect(status().isBadRequest());
		nota(1, "3.55").andExpect(status().isBadRequest());
		nota(4, "3.0").andExpect(status().isBadRequest());
		registrar(profesor, inscripcion.getId(), "{}").andExpect(status().isBadRequest());
		assertThat(fila().get("nota_corte1")).isNull();
		nota(1, "5.0").andExpect(status().isCreated());
	}

	/** Otro profesor no califica el grupo ajeno: 403 auditado. Una inscripción inexistente da 404. */
	@Test
	void soloElProfesorDelGrupo() throws Exception {
		registrar(otroProfesor, inscripcion.getId(), "{\"corte\":1,\"valor\":3.0}").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
		registrar(profesor, 999_999_999L, "{\"corte\":1,\"valor\":3.0}").andExpect(status().isNotFound());
		assertThat(fila().get("nota_corte1")).isNull();
		assertThat(auditorias("FALLIDO")).isEqualTo(1);
	}

	/** Si el periodo del grupo ya no está activo, no se califica: 422. */
	@Test
	void soloEnElPeriodoActivo() throws Exception {
		jdbc.update("update periodo set activo = false, marca_activo = null where id = ?", activo.getId());
		nota(1, "3.0").andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("PERIODO_NO_ACTIVO"));
		assertThat(fila().get("nota_corte1")).isNull();
	}

	/** Un estudiante no registra notas y sin token no se entra. */
	@Test
	void soloElRolProfesor() throws Exception {
		registrar(estudiante, inscripcion.getId(), "{\"corte\":1,\"valor\":5.0}").andExpect(status().isForbidden());
		mvc.perform(post("/api/profesor/inscripciones/" + inscripcion.getId() + "/notas")
						.contentType(MediaType.APPLICATION_JSON).content("{\"corte\":1,\"valor\":5.0}"))
				.andExpect(status().isUnauthorized());
		assertThat(fila().get("nota_corte1")).isNull();
	}
}
