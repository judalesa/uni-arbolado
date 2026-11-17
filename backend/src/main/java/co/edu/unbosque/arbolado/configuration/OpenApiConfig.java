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
