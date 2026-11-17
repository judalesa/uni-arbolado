package co.edu.unbosque.arbolado.configuration;

import co.edu.unbosque.arbolado.security.ManejadorErroresSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadena de seguridad HTTP de la API.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * Define las reglas de acceso, el CORS y las respuestas de error en formato JSON.
	 *
	 * @param http      constructor de la cadena de seguridad
	 * @param manejador responde 401 y 403 con el formato {@code ErrorResponse}
	 * @return la cadena de filtros de seguridad
	 * @throws Exception si la configuración es inválida
	 */
	@Bean
	public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ManejadorErroresSeguridad manejador)
			throws Exception {
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
				.authorizeHttpRequests(reglas -> reglas
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// Solo responden si springdoc está encendido en la configuración local.
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated());
		return http.build();
	}

	/**
	 * Marcador temporal: evita que Spring Boot cree un usuario de prueba con una contraseña aleatoria
	 * impresa en el registro. Se reemplaza en FT-04 por el servicio que consulta la tabla de usuarios.
	 *
	 * @return un servicio sin usuarios
	 */
	@Bean
	public UserDetailsService servicioDeUsuariosTemporal() {
		return correo -> {
			throw new UsernameNotFoundException("Aún no hay autenticación configurada");
		};
	}
}
