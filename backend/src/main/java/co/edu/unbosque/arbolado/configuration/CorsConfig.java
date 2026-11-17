package co.edu.unbosque.arbolado.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Política de CORS: qué páginas web (orígenes) pueden llamar a esta API desde el navegador.
 */
@Configuration
public class CorsConfig {

	/**
	 * Construye la fuente de configuración CORS que Spring Security usa en cada petición.
	 *
	 * @param propiedades configuración validada de la aplicación
	 * @return la política CORS aplicada a todas las rutas
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(PropiedadesApp propiedades) {
		CorsConfiguration politica = new CorsConfiguration();
		// Orígenes exactos (nunca "*"): la API usa cabecera Authorization.
		politica.setAllowedOrigins(propiedades.cors().allowedOrigins().stream().map(String::trim).toList());
		politica.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
		politica.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		// El navegador recuerda la respuesta de la verificación previa durante una hora.
		politica.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", politica);
		return fuente;
	}
}
