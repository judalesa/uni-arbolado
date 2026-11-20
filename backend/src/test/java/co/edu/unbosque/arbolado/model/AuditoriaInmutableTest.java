package co.edu.unbosque.arbolado.model;

import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import jakarta.persistence.Column;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba por reflexión que la auditoría no ofrece ninguna forma de editarse ni borrarse. */
class AuditoriaInmutableTest {

	/** La entidad está marcada como inmutable para Hibernate. */
	@Test
	void laEntidadEsInmutable() {
		assertThat(Auditoria.class.isAnnotationPresent(Immutable.class)).isTrue();
	}

	/** Todas las columnas, salvo el identificador, son no actualizables. */
	@Test
	void ningunaColumnaEsActualizable() {
		for (Field campo : Auditoria.class.getDeclaredFields()) {
			Column columna = campo.getAnnotation(Column.class);
			if (columna != null) {
				assertThat(columna.updatable()).as(campo.getName()).isFalse();
			}
		}
	}

	/** La entidad no tiene métodos que modifiquen su estado. */
	@Test
	void laEntidadNoTieneSetters() {
		assertThat(Arrays.stream(Auditoria.class.getDeclaredMethods()).map(Method::getName))
				.noneMatch(nombre -> nombre.startsWith("set"));
	}

	/** El repositorio no hereda operaciones de borrado ni de actualización. */
	@Test
	void elRepositorioNoOfreceBorrarNiActualizar() {
		assertThat(CrudRepository.class.isAssignableFrom(AuditoriaRepository.class)).isFalse();
		assertThat(JpaRepository.class.isAssignableFrom(AuditoriaRepository.class)).isFalse();
		assertThat(Arrays.stream(AuditoriaRepository.class.getMethods()).map(Method::getName))
				.noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove")
						|| nombre.startsWith("update") || nombre.startsWith("set"));
	}

	/** Cada registro se crea con la hora actual y los datos recibidos. */
	@Test
	void elRegistroConservaLosDatosRecibidos() {
		Auditoria registro = new Auditoria(AccionAuditoria.INSCRIPCION, 5L, "a@b.com", "INSCRIPCION", "9",
				Resultado.EXITOSO, "ok");

		assertThat(registro.getAccion()).isEqualTo(AccionAuditoria.INSCRIPCION);
		assertThat(registro.getUsuarioId()).isEqualTo(5L);
		assertThat(registro.getIdRecurso()).isEqualTo("9");
		assertThat(registro.getFechaHora()).isNotNull();
		assertThat(registro.getId()).isNull();
	}
}
