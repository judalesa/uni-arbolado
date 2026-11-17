package co.edu.unbosque.arbolado.configuration;

import co.edu.unbosque.arbolado.security.JwtAuthenticationFilter;
import co.edu.unbosque.arbolado.security.JwtService;
import co.edu.unbosque.arbolado.security.ManejadorErroresSeguridad;
import co.edu.unbosque.arbolado.service.TokenRevocadoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad HTTP de la API.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * Define las reglas de acceso, el CORS, el filtro JWT y las respuestas de error en formato JSON.
	 * Los permisos por rol se aplican además con {@code @PreAuthorize} en cada controlador.
	 *
	 * @param http      constructor de la cadena de seguridad
	 * @param manejador responde 401 y 403 con el formato {@code ErrorResponse}
	 * @param jwt       servicio que valida los tokens
	 * @param revocados lista de tokens cerrados con el logout
	 * @return la cadena de filtros de seguridad
	 * @throws Exception si la configuración es inválida
	 */
	@Bean
	public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ManejadorErroresSeguridad manejador,
												 JwtService jwt, TokenRevocadoService revocados) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.exceptionHandling(e -> e
						.authenticationEntryPoint(manejador)
						.accessDeniedHandler(manejador))
				.addFilterBefore(new JwtAuthenticationFilter(jwt, revocados), UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(reglas -> reglas
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro",
								"/api/auth/verificar", "/api/auth/reenviar-codigo").permitAll()
						// Solo responden si springdoc está encendido en la configuración local.
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated());
		return http.build();
	}
}
