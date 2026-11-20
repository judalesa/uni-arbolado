package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.DiaSemana;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Horario;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Comprueba que la base de datos misma protege el número del grupo, el horario y el cupo. */
@SpringBootTest
@ActiveProfiles("test")
class GrupoRepositoryTest {

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
	 * Guarda un profesor con documento y correo propios de esta clase.
	 *
	 * @param n número que vuelve único el documento y el correo
	 * @return el profesor
	 */
	private Usuario profesor(int n) {
		return usuarios.save(new Usuario("55000000" + n, "Profesor " + n, "repo" + n + "@example.com", "x", Rol.PROFESOR,
				EstadoCuenta.ACTIVO));
	}

	/** Dos grupos con el mismo número en la misma asignatura y periodo violan la restricción única. */
	@Test
	void elNumeroEsUnicoPorAsignaturaYPeriodo() {
		Periodo periodo = periodo("6000-1");
		Usuario profesor = profesor(10);
		var mat = asignaturas.findByCodigo("MAT101").orElseThrow();
		grupos.saveAndFlush(new Grupo(1, mat, periodo, profesor, "B", "301"));

		assertThatThrownBy(() -> grupos.saveAndFlush(new Grupo(1, mat, periodo, profesor, "B", "302")))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThat(grupos.siguienteNumero(mat.getId(), periodo.getId())).isEqualTo(2);
	}

	/** Una franja que termina antes de empezar viola la restricción CHECK de la base. */
	@Test
	void laBaseRechazaUnHorarioInvertido() {
		Periodo periodo = periodo("6000-2");
		var mat = asignaturas.findByCodigo("ALG201").orElseThrow();
		Grupo grupo = new Grupo(1, mat, periodo, profesor(11), "B", "301");
		grupo.reemplazarHorarios(List.of(new Horario(grupo, DiaSemana.LUNES, LocalTime.of(10, 0), LocalTime.of(8, 0))));

		assertThatThrownBy(() -> grupos.saveAndFlush(grupo)).isInstanceOf(DataIntegrityViolationException.class);
	}

	/** Todo grupo nace con cupo 30. */
	@Test
	void todoGrupoNaceConCupo30() {
		Periodo periodo = periodo("6000-3");
		var mat = asignaturas.findByCodigo("DSI101").orElseThrow();

		Grupo guardado = grupos.saveAndFlush(new Grupo(1, mat, periodo, profesor(12), "B", "301"));

		assertThat(guardado.getCupoMaximo()).isEqualTo(Grupo.CUPO_MAXIMO).isEqualTo(30);
	}
}
