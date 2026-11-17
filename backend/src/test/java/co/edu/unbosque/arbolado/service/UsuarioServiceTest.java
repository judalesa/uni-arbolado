package co.edu.unbosque.arbolado.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de la preparación del texto de búsqueda de usuarios. */
class UsuarioServiceTest {

	/** Sin texto no hay filtro. */
	@Test
	void sinTextoNoFiltra() {
		assertThat(UsuarioService.prepararBusqueda(null)).isEmpty();
		assertThat(UsuarioService.prepararBusqueda("   ")).isEmpty();
	}

	/** El texto se recorta y se pasa a minúsculas. */
	@Test
	void recortaYPasaAMinusculas() {
		assertThat(UsuarioService.prepararBusqueda("  María PÉREZ ")).isEqualTo("maría pérez");
	}

	/** Los comodines de LIKE y el propio carácter de escape se escapan. */
	@Test
	void escapaLosComodines() {
		assertThat(UsuarioService.prepararBusqueda("50%_!")).isEqualTo("50!%!_!!");
	}
}
