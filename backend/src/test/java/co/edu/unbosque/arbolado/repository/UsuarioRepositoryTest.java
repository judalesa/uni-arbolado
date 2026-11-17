package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de la persistencia de usuarios y de sus restricciones de unicidad (RN-01). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioRepositoryTest {

	/** Repositorio bajo prueba. */
	@Autowired
	private UsuarioRepository repositorio;

	/**
	 * Crea una cuenta de estudiante de ejemplo.
	 *
	 * @param documento documento de la cuenta
	 * @param correo    correo de la cuenta
	 * @return la cuenta sin guardar
	 */
	private Usuario estudiante(String documento, String correo) {
		return new Usuario(documento, "Ana Pérez", correo, "clave-codificada", Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE);
	}

	/** Al guardar se asigna el identificador, se normaliza el correo y se fija la fecha de creación. */
	@Test
	void guardaYNormalizaElCorreo() {
		Usuario guardado = repositorio.saveAndFlush(estudiante("1000000001", "  Ana.Perez@Correo.COM "));

		assertThat(guardado.getId()).isNotNull();
		assertThat(guardado.getCorreo()).isEqualTo("ana.perez@correo.com");
		assertThat(guardado.getFechaCreacion()).isNotNull();
		assertThat(repositorio.findByCorreo("ana.perez@correo.com")).isPresent();
	}

	/** El documento conserva los ceros a la izquierda. */
	@Test
	void elDocumentoConservaLosCerosAlInicio() {
		repositorio.saveAndFlush(estudiante("0000000042", "cero@correo.com"));

		assertThat(repositorio.existsByDocumento("0000000042")).isTrue();
		assertThat(repositorio.findByCorreo("cero@correo.com").orElseThrow().getDocumento()).isEqualTo("0000000042");
	}

	/** No se admiten dos cuentas con el mismo documento. */
	@Test
	void rechazaUnDocumentoRepetido() {
		repositorio.saveAndFlush(estudiante("1000000002", "uno@correo.com"));

		assertThatThrownBy(() -> repositorio.saveAndFlush(estudiante("1000000002", "otro@correo.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/** No se admiten dos cuentas con el mismo correo. */
	@Test
	void rechazaUnCorreoRepetido() {
		repositorio.saveAndFlush(estudiante("1000000003", "mismo@correo.com"));

		assertThatThrownBy(() -> repositorio.saveAndFlush(estudiante("1000000004", "MISMO@correo.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/** Activar la cuenta cambia el estado. */
	@Test
	void activarCambiaElEstado() {
		Usuario usuario = estudiante("1000000005", "activar@correo.com");
		usuario.activar();

		assertThat(usuario.getEstado()).isEqualTo(EstadoCuenta.ACTIVO);
	}

	/** La representación en texto no revela la contraseña. */
	@Test
	void toStringNoMuestraLaContrasena() {
		Usuario usuario = estudiante("1000000006", "texto@correo.com");

		assertThat(usuario.toString()).doesNotContain("clave-codificada").contains("texto@correo.com");
	}
}
