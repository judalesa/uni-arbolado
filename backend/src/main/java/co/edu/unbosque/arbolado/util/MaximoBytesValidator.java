package co.edu.unbosque.arbolado.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/** Valida {@link MaximoBytes}: cuenta los bytes UTF-8 del texto. */
public class MaximoBytesValidator implements ConstraintValidator<MaximoBytes, CharSequence> {

	/** Máximo de bytes permitido. */
	private int maximo;

	/**
	 * Toma el máximo de la anotación.
	 *
	 * @param anotacion restricción declarada en el campo
	 */
	@Override
	public void initialize(MaximoBytes anotacion) {
		this.maximo = anotacion.value();
	}

	/**
	 * Comprueba el tamaño en bytes; un valor nulo es válido (de eso se encarga {@code @NotBlank}).
	 *
	 * @param valor   texto a comprobar
	 * @param context contexto de la validación
	 * @return {@code true} si no supera el máximo
	 */
	@Override
	public boolean isValid(CharSequence valor, ConstraintValidatorContext context) {
		return valor == null || valor.toString().getBytes(StandardCharsets.UTF_8).length <= maximo;
	}
}
