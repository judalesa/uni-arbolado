package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.service.TokenRevocadoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el JWT de la cabecera {@code Authorization: Bearer} y, si es válido y no está revocado, deja la
 * identidad en el contexto de seguridad. Un token inválido no se rechaza aquí: la petición sigue sin
 * identidad y las reglas de acceso responden 401 si la ruta lo exige, así las rutas públicas funcionan
 * aunque el cliente envíe un token vencido. No se registra como bean para que Spring Boot no lo agregue
 * una segunda vez a la cadena de servlets.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	/** Prefijo del esquema de autenticación. */
	private static final String PREFIJO = "Bearer ";

	/** Servicio que valida el token. */
	private final JwtService jwt;

	/** Servicio que consulta la lista de tokens revocados. */
	private final TokenRevocadoService revocados;

	/**
	 * Crea el filtro.
	 *
	 * @param jwt       servicio que valida el token
	 * @param revocados lista de tokens revocados
	 */
	public JwtAuthenticationFilter(JwtService jwt, TokenRevocadoService revocados) {
		this.jwt = jwt;
		this.revocados = revocados;
	}

	/**
	 * Autentica la petición si trae un token válido.
	 *
	 * @param request  petición en curso
	 * @param response respuesta en curso
	 * @param chain    resto de la cadena de filtros
	 * @throws ServletException si falla un filtro posterior
	 * @throws IOException      si falla la lectura o escritura
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (cabecera != null && cabecera.startsWith(PREFIJO)) {
			jwt.validar(cabecera.substring(PREFIJO.length()).trim())
					.filter(usuario -> !revocados.estaRevocado(usuario.jti()))
					.ifPresent(usuario -> {
						var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null,
								List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
						SecurityContextHolder.getContext().setAuthentication(autenticacion);
					});
		}
		chain.doFilter(request, response);
	}
}
