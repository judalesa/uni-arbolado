package co.edu.unbosque.arbolado.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuración propia de la aplicación (prefijo {@code app}), leída de
 * {@code application-local.properties}.
 *
 * @param seguridad parámetros de JWT, cifrado de contraseñas y códigos de verificación
 * @param admin     datos del administrador precargado (RN-05)
 * @param cors      orígenes del frontend autorizados
 * @param correo    modo de envío de correos
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record PropiedadesApp(
		@Valid @NotNull Seguridad seguridad,
		@Valid @NotNull Admin admin,
		@Valid @NotNull Cors cors,
		@Valid @NotNull Correo correo) {

	/**
	 * Secretos y parámetros de seguridad.
	 *
	 * @param jwtSecreto            llave HS256 del JWT (al menos 32 caracteres)
	 * @param jwtExpiracionMinutos  vigencia del token en minutos (decisión D-33: 60)
	 * @param aesLlaveBase64        llave AES de 256 bits codificada en Base64
	 * @param codigoHmacSecreto     secreto con el que se firma (HMAC) el código de verificación
	 */
	public record Seguridad(
			@NotBlank @Size(min = 32, message = "debe tener al menos 32 caracteres") String jwtSecreto,
			@Min(1) @Max(1440) int jwtExpiracionMinutos,
			@NotBlank String aesLlaveBase64,
			@NotBlank String codigoHmacSecreto) {
	}

	/**
	 * Administrador inicial. Sus credenciales salen de la configuración local, nunca del código.
	 *
	 * @param documento  documento de 10 dígitos
	 * @param nombre     nombre completo
	 * @param correo     correo electrónico
	 * @param contrasena clave inicial (8 a 72 caracteres)
	 */
	public record Admin(
			@NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "debe tener exactamente 10 dígitos") String documento,
			@NotBlank String nombre,
			@NotBlank @Email String correo,
			@NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String contrasena) {
	}

	/**
	 * Política de CORS.
	 *
	 * @param allowedOrigins orígenes exactos con esquema y sin comodines (por ejemplo el sitio de Netlify),
	 *                       separados por coma; un {@code *} impide que la aplicación arranque
	 */
	public record Cors(@NotEmpty List<@NotBlank @Pattern(regexp = "^https?://[^*\\s/]+$",
			message = "debe ser un origen exacto, con http o https y sin comodines ni barra final") String> allowedOrigins) {
	}

	/**
	 * Envío de correos.
	 *
	 * @param modo      {@code LOG} imprime el código en el registro (desarrollo); {@code SMTP} envía de verdad
	 * @param remitente dirección que aparece como remitente
	 */
	public record Correo(@NotNull ModoCorreo modo, @NotBlank String remitente) {
	}

	/** Forma en que la aplicación entrega los correos. */
	public enum ModoCorreo {
		/** Escribe el contenido en el registro; no envía nada. Útil en desarrollo y pruebas. */
		LOG,
		/** Envía el correo por SMTP. */
		SMTP
	}
}
