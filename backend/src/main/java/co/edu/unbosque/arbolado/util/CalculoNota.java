package co.edu.unbosque.arbolado.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo de la nota definitiva (RN-24 a RN-27). Usa {@link BigDecimal} porque con {@code double} un 2.95
 * puede quedar en 2.9499… y reprobar a quien aprobó.
 */
public final class CalculoNota {

	/** Peso del primer corte. */
	static final BigDecimal PESO_CORTE_1 = new BigDecimal("0.30");

	/** Peso del segundo corte. */
	static final BigDecimal PESO_CORTE_2 = new BigDecimal("0.30");

	/** Peso del tercer corte. */
	static final BigDecimal PESO_CORTE_3 = new BigDecimal("0.40");

	/** Nota mínima para aprobar, comparada con la definitiva ya redondeada (RN-27). */
	public static final BigDecimal NOTA_APROBATORIA = new BigDecimal("3.0");

	/** Clase de utilidades: no se instancia. */
	private CalculoNota() {
	}

	/**
	 * Calcula la definitiva 30/30/40 redondeada a un decimal con {@link RoundingMode#HALF_UP} (RN-26).
	 *
	 * @param corte1 nota del primer corte
	 * @param corte2 nota del segundo corte
	 * @param corte3 nota del tercer corte
	 * @return la definitiva con un decimal
	 */
	public static BigDecimal definitiva(BigDecimal corte1, BigDecimal corte2, BigDecimal corte3) {
		return corte1.multiply(PESO_CORTE_1).add(corte2.multiply(PESO_CORTE_2)).add(corte3.multiply(PESO_CORTE_3))
				.setScale(1, RoundingMode.HALF_UP);
	}

	/**
	 * Indica si una definitiva aprueba.
	 *
	 * @param definitiva nota ya redondeada
	 * @return {@code true} si es 3.0 o más
	 */
	public static boolean aprueba(BigDecimal definitiva) {
		return definitiva.compareTo(NOTA_APROBATORIA) >= 0;
	}
}
