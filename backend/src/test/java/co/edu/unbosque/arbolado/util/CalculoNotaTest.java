package co.edu.unbosque.arbolado.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas del cálculo de la definitiva: pesos, redondeo HALF_UP y aprobación en 3.0. */
class CalculoNotaTest {

	/**
	 * La definitiva pondera 30/30/40 y redondea a un decimal con la mitad hacia arriba.
	 *
	 * @param c1       primer corte
	 * @param c2       segundo corte
	 * @param c3       tercer corte
	 * @param esperada definitiva esperada
	 */
	@ParameterizedTest
	@CsvSource({
			"3.0, 3.0, 2.9, 3.0", // 2.96 sube
			"2.9, 2.9, 3.0, 2.9", // 2.94 baja
			"2.0, 2.5, 4.0, 3.0", // 2.95 exacto sube
			"5.0, 5.0, 5.0, 5.0",
			"0.0, 0.0, 0.0, 0.0",
			"4.0, 3.0, 1.0, 2.5"})
	void definitivaPonderadaYRedondeada(String c1, String c2, String c3, String esperada) {
		BigDecimal definitiva = CalculoNota.definitiva(new BigDecimal(c1), new BigDecimal(c2), new BigDecimal(c3));
		assertThat(definitiva).isEqualByComparingTo(esperada);
		assertThat(definitiva.scale()).isEqualTo(1);
	}

	/** Se aprueba desde 3.0 exacto; 2.9 reprueba. */
	@Test
	void apruebaDesdeTres() {
		assertThat(CalculoNota.aprueba(new BigDecimal("3.0"))).isTrue();
		assertThat(CalculoNota.aprueba(new BigDecimal("5.0"))).isTrue();
		assertThat(CalculoNota.aprueba(new BigDecimal("2.9"))).isFalse();
	}

	/** Los pesos suman 100 %. */
	@Test
	void pesosSumanUno() {
		assertThat(CalculoNota.PESO_CORTE_1.add(CalculoNota.PESO_CORTE_2).add(CalculoNota.PESO_CORTE_3))
				.isEqualByComparingTo(BigDecimal.ONE);
	}
}
