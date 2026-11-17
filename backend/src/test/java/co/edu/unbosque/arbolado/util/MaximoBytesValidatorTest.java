package co.edu.unbosque.arbolado.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas del límite de bytes (TD-04): los caracteres con tilde o ñ ocupan dos bytes. */
class MaximoBytesValidatorTest {

	/** Validador configurado con un máximo de 72 bytes. */
	private final MaximoBytesValidator validador = crear();

	/**
	 * Crea el validador con la anotación simulada.
	 *
	 * @return validador con máximo 72
	 */
	private static MaximoBytesValidator crear() {
		MaximoBytes anotacion = mock(MaximoBytes.class);
		when(anotacion.value()).thenReturn(72);
		MaximoBytesValidator v = new MaximoBytesValidator();
		v.initialize(anotacion);
		return v;
	}

	/** Nulo y texto de exactamente 72 bytes son válidos. */
	@Test
	void aceptaNuloYElLimiteExacto() {
		assertThat(validador.isValid(null, null)).isTrue();
		assertThat(validador.isValid("a".repeat(72), null)).isTrue();
		assertThat(validador.isValid("ñ".repeat(36), null)).isTrue();
	}

	/** Un byte de más se rechaza, aunque los caracteres sean pocos. */
	@Test
	void rechazaMasDe72Bytes() {
		assertThat(validador.isValid("a".repeat(73), null)).isFalse();
		assertThat(validador.isValid("ñ".repeat(37), null)).isFalse();
	}
}
