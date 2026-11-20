package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.DiaSemana;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.EstadoInscripcion;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Horario;
import co.edu.unbosque.arbolado.model.Inscripcion;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.service.InscritosDeGrupo;
import co.edu.unbosque.arbolado.service.PendientesDePeriodo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Comprueba que la base protege las inscripciones y que los conteos que usan otros servicios son reales. */
@SpringBootTest
@ActiveProfiles("test")
class InscripcionRepositoryTest {

	/** Repositorio de inscripciones. */
	@Autowired
	private InscripcionRepository inscripciones;

	/** Repositorio de grupos. */
	@Autowired
	private GrupoRepository grupos;

	/** Repositorio de periodos. */
	@Autowired
	private PeriodoRepository periodos;

	/** Repositorio de asignaturas. */
	@Autowired
	private AsignaturaRepository asignaturas;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Conteo de inscritos por grupo. */
	@Autowired
	private InscritosDeGrupo inscritos;

	/** Conteo de inscripciones pendientes por periodo. */
	@Autowired
	private PendientesDePeriodo pendientes;

	/** Acceso directo a la base. */
	@Autowired
	private JdbcTemplate jdbc;

	/**
	 * Guarda un periodo con código propio de esta clase.
	 *
	 * @param codigo código del periodo
	 * @return el periodo
	 */
	private Periodo periodo(String codigo) {
		return periodos.save(new Periodo(codigo, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30)));
	}

	/**
	 * Guarda una cuenta con documento y correo propios de esta clase.
	 *
	 * @param n    número que vuelve únicos el documento y el correo
	 * @param rol  rol de la cuenta
	 * @return la cuenta
	 */
	private Usuario cuenta(int n, Rol rol) {
		return usuarios.save(new Usuario("56000000" + n, "Cuenta " + n, "insrepo" + n + "@example.com", "x", rol,
				EstadoCuenta.ACTIVO));
	}

	/**
	 * Guarda un grupo con una franja.
	 *
	 * @param periodo  periodo
	 * @param codigo   asignatura
	 * @param profesor profesor
	 * @return el grupo
	 */
	private Grupo grupo(Periodo periodo, String codigo, Usuario profesor) {
		var asignatura = asignaturas.findByCodigo(codigo).orElseThrow();
		Grupo grupo = new Grupo(grupos.siguienteNumero(asignatura.getId(), periodo.getId()), asignatura, periodo, profesor,
				"B", "301");
		grupo.reemplazarHorarios(List.of(new Horario(grupo, DiaSemana.LUNES, LocalTime.of(8, 0), LocalTime.of(10, 0))));
		return grupos.save(grupo);
	}

	/** Nadie se inscribe dos veces en el mismo grupo. */
	@Test
	void unEstudianteNoRepiteGrupo() {
		Periodo periodo = periodo("8000-1");
		Usuario profesor = cuenta(10, Rol.PROFESOR);
		Usuario estudiante = cuenta(11, Rol.ESTUDIANTE);
		Grupo grupo = grupo(periodo, "DSI101", profesor);
		inscripciones.saveAndFlush(new Inscripcion(estudiante, grupo));

		assertThatThrownBy(() -> inscripciones.saveAndFlush(new Inscripcion(estudiante, grupo)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/** Dos inscripciones vigentes de la misma asignatura en grupos distintos las rechaza la propia base. */
	@Test
	void laBaseRechazaDosInscripcionesVigentesDeLaMismaAsignatura() {
		Periodo periodo = periodo("8000-2");
		Usuario profesor = cuenta(20, Rol.PROFESOR);
		Usuario estudiante = cuenta(21, Rol.ESTUDIANTE);
		Grupo uno = grupo(periodo, "DSI101", profesor);
		Grupo dos = grupo(periodo, "DSI101", profesor);
		inscripciones.saveAndFlush(new Inscripcion(estudiante, uno));

		assertThatThrownBy(() -> inscripciones.saveAndFlush(new Inscripcion(estudiante, dos)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/** Una reprobada deja de ser vigente (marca nula) y no impide reinscribir la asignatura. */
	@Test
	void unaReprobadaNoImpideReinscribir() {
		Periodo anterior = periodo("8000-3");
		Periodo nuevo = periodo("8000-4");
		Usuario profesor = cuenta(30, Rol.PROFESOR);
		Usuario estudiante = cuenta(31, Rol.ESTUDIANTE);
		Inscripcion primera = inscripciones.saveAndFlush(new Inscripcion(estudiante, grupo(anterior, "DSI101", profesor)));
		jdbc.update("update inscripcion set estado = 'REPROBADA', marca_vigente = null where id = ?", primera.getId());

		Inscripcion segunda = inscripciones.saveAndFlush(new Inscripcion(estudiante, grupo(nuevo, "DSI101", profesor)));

		assertThat(segunda.getId()).isNotEqualTo(primera.getId());
	}

	/** El periodo y la asignatura de la inscripción se copian del grupo al crearla. */
	@Test
	void copiaElPeriodoYLaAsignaturaDelGrupo() {
		Periodo periodo = periodo("8000-5");
		Usuario profesor = cuenta(40, Rol.PROFESOR);
		Grupo grupo = grupo(periodo, "ING101", profesor);

		Inscripcion inscripcion = new Inscripcion(cuenta(41, Rol.ESTUDIANTE), grupo);

		assertThat(inscripcion.getPeriodo()).isSameAs(periodo);
		assertThat(inscripcion.getAsignatura()).isSameAs(grupo.getAsignatura());
		assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.CURSANDO);
	}

	/** Los conteos reales: cursando por grupo, total por grupo y pendientes por periodo. */
	@Test
	void losConteosSonReales() {
		Periodo periodo = periodo("8000-6");
		Usuario profesor = cuenta(50, Rol.PROFESOR);
		Grupo grupo = grupo(periodo, "PSI101", profesor);
		Inscripcion cursando = inscripciones.save(new Inscripcion(cuenta(51, Rol.ESTUDIANTE), grupo));
		Inscripcion cerrada = inscripciones.save(new Inscripcion(cuenta(52, Rol.ESTUDIANTE), grupo));
		jdbc.update("update inscripcion set estado = 'APROBADA' where id = ?", cerrada.getId());

		Map<Long, Long> porGrupo = inscritos.cursandoPorGrupo(List.of(grupo.getId()));

		assertThat(cursando.getId()).isNotNull();
		assertThat(porGrupo).containsEntry(grupo.getId(), 1L);
		assertThat(inscritos.contarInscripciones(grupo.getId())).isEqualTo(2);
		assertThat(inscritos.contarCursando(grupo.getId())).isEqualTo(1);
		assertThat(pendientes.contarSinDefinitiva(periodo.getId())).isEqualTo(1);
		assertThat(inscritos.cursandoPorGrupo(List.of())).isEmpty();
	}
}
