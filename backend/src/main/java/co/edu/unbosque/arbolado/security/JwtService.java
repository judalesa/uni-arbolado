package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Genera y valida los JWT de la API: HS256, con el correo como {@code sub}, el rol, el identificador del
 * usuario y un {@code jti} único para poder revocarlos.
 */
@Component
public class JwtService {

	/** Nombre del claim con el rol. */
	static final String CLAIM_ROL = "rol";

	/** Nombre del claim con el identificador del usuario. */
	static final String CLAIM_ID = "uid";

	/** Llave de firma derivada del secreto de configuración. */
	private final SecretKey llave;

	/** Vigencia de cada token. */
	private final Duration vigencia;

	/** Reloj, inyectable para probar la expiración sin esperar. */
	private final Clock reloj;

	/**
	 * Crea el servicio con el reloj del sistema.
	 *
	 * @param propiedades configuración de la aplicación
	 */
	@Autowired
	public JwtService(PropiedadesApp propiedades) {
		this(propiedades, Clock.systemUTC());
	}

	/**
	 * Crea el servicio con un reloj propio.
	 *
	 * @param propiedades configuración de la aplicación
	 * @param reloj       fuente de la hora actual
	 */
	JwtService(PropiedadesApp propiedades, Clock reloj) {
		this.llave = Keys.hmacShaKeyFor(propiedades.seguridad().jwtSecreto().getBytes(StandardCharsets.UTF_8));
		this.vigencia = Duration.ofMinutes(propiedades.seguridad().jwtExpiracionMinutos());
		this.reloj = reloj;
	}

	/**
	 * Emite un token para el usuario.
	 *
	 * @param usuario cuenta que inició sesión
	 * @return el token ya firmado, con su identificador y su vencimiento
	 */
	public TokenEmitido emitir(Usuario usuario) {
		Instant ahora = reloj.instant();
		Instant expira = ahora.plus(vigencia);
		String jti = UUID.randomUUID().toString();
		String token = Jwts.builder()
				.subject(usuario.getCorreo())
				.id(jti)
				.claim(CLAIM_ROL, usuario.getRol().name())
				.claim(CLAIM_ID, usuario.getId())
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(expira))
				.signWith(llave, Jwts.SIG.HS256)
				.compact();
		return new TokenEmitido(token, jti, expira);
	}

	/**
	 * Valida la firma, el vencimiento y los claims del token.
	 *
	 * @param token texto del token, sin el prefijo {@code Bearer}
	 * @return la identidad del token, o vacío si es inválido, está vencido o le faltan datos
	 */
	public Optional<UsuarioAutenticado> validar(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(llave)
					.clock(() -> Date.from(reloj.instant()))
					.build()
					.parseSignedClaims(token)
					.getPayload();
			Number id = claims.get(CLAIM_ID, Number.class);
			String rol = claims.get(CLAIM_ROL, String.class);
			if (id == null || rol == null || claims.getSubject() == null || claims.getId() == null
					|| claims.getExpiration() == null) {
				return Optional.empty();
			}
			return Optional.of(new UsuarioAutenticado(id.longValue(), claims.getSubject(), Rol.valueOf(rol),
					claims.getId(), claims.getExpiration().toInstant()));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/**
	 * Token recién emitido.
	 *
	 * @param token    texto firmado que recibe el cliente
	 * @param jti      identificador único del token
	 * @param expiraEn instante de vencimiento
	 */
	public record TokenEmitido(String token, String jti, Instant expiraEn) {
	}
}
