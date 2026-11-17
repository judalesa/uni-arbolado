package co.edu.unbosque.arbolado.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias de la regla "una asignatura no es su propio prerrequisito". */
class PrerrequisitoTest {

	/** Una relación entre dos asignaturas distintas se crea sin problema. */
	@Test
	void permiteDosAsignaturasDistintas() {
		Asignatura matematicas = new Asignatura("MAT101", "Matemáticas 1", "Cálculo.", 1, 3);
		Asignatura algebra = new Asignatura("ALG201", "Álgebra Lineal", "Matrices.", 2, 2);

		Prerrequisito relacion = new Prerrequisito(algebra, matematicas);

		assertThat(relacion.getAsignatura()).isSameAs(algebra);
		assertThat(relacion.getRequisito()).isSameAs(matematicas);
	}

	/** Una asignatura no puede exigirse a sí misma. */
	@Test
	void rechazaUnAutoRequisito() {
		Asignatura matematicas = new Asignatura("MAT101", "Matemáticas 1", "Cálculo.", 1, 3);

		assertThatThrownBy(() -> new Prerrequisito(matematicas, matematicas))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
