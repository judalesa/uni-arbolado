package co.edu.unbosque.arbolado.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación interactiva (Swagger UI) generada por springdoc.
 *
 * <p>La documentación está apagada por defecto ({@code springdoc.*.enabled=false}) para no exponer la
 * API en el servidor compartido; se enciende en {@code application-local.properties} cuando se necesita.
 * El contrato oficial entre backend y frontend es {@code openapi.yaml}; esta salida es la versión viva
 * que se compara con él.</p>
 */
@Configuration
public class OpenApiConfig {

	/** Nombre del esquema de seguridad que Swagger muestra en el botón "Authorize". */
	public static final String ESQUEMA_JWT = "bearerAuth";

	/**
	 * Describe la API y el esquema de autenticación con token JWT.
	 *
	 * @return definición base de OpenAPI
	 */
	@Bean
	public OpenAPI definicionApi() {
		return new OpenAPI()
				.info(new Info()
						.title("API del Sistema de Gestión Académica en Línea - Universidad El Arbolado")
						.version("1.0.0")
						.description("Backend del proyecto final de DS3. Autenticación con JWT en la cabecera Authorization."))
				.components(new Components().addSecuritySchemes(ESQUEMA_JWT,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
	}
}
