package co.edu.unbosque.arbolado.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Cuerpo de {@code POST /api/profesor/inscripciones/{id}/notas}: el corte y su nota (RN-24). */
public class NotaRequest {

	/** Corte: 1, 2 o 3. */
	@NotNull(message = "El corte es obligatorio")
	@Min(value = 1, message = "El corte debe ser 1, 2 o 3")
	@Max(value = 3, message = "El corte debe ser 1, 2 o 3")
	private Integer corte;

	/** Nota entre 0.0 y 5.0 con un decimal. */
	@NotNull(message = "La nota es obligatoria")
	@DecimalMin(value = "0.0", message = "La nota debe estar entre 0.0 y 5.0")
	@DecimalMax(value = "5.0", message = "La nota debe estar entre 0.0 y 5.0")
	@Digits(integer = 1, fraction = 1, message = "La nota admite un solo decimal")
	private BigDecimal valor;

	/** Constructor sin argumentos que exige Jackson. */
	public NotaRequest() {
	}

	/** @return corte */
	public Integer getCorte() {
		return corte;
	}

	/** @param corte corte */
	public void setCorte(Integer corte) {
		this.corte = corte;
	}

	/** @return nota */
	public BigDecimal getValor() {
		return valor;
	}

	/** @param valor nota */
	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}
}
