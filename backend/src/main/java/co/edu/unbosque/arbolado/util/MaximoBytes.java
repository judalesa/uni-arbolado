package co.edu.unbosque.arbolado.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El texto no puede ocupar más de {@link #value()} bytes en UTF-8. BCrypt solo procesa 72 bytes y una
 * clave con tildes o ñ ocupa más bytes que caracteres.
 */
@Documented
@Constraint(validatedBy = MaximoBytesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MaximoBytes {

	/** @return máximo de bytes permitido */
	int value();

	/** @return mensaje de error */
	String message() default "No puede superar {value} bytes";

	/** @return grupos de validación */
	Class<?>[] groups() default {};

	/** @return carga útil de la validación */
	Class<? extends Payload>[] payload() default {};
}
