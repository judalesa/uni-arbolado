package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de generación y validación del JWT. */
class JwtServiceTest {

	/** Instante fijo para que las pruebas no dependan del reloj. */
	private static final Instant AHORA = Instant.parse("2026-11-17T15:00:00Z");

	/** Servicio bajo prueba, con reloj fijo. */
	private final JwtService servicio = new JwtService(PropiedadesDePrueba.estandar(),
			Clock.fixed(AHORA, ZoneOffset.UTC));

	/** Cuenta de ejemplo; el identificador lo asigna la base de datos, aquí se simula por reflexión. */
	private final Usuario usuario = cuentaDeEjemplo();

	/**
	 * Crea la cuenta de ejemplo con un identificador asignado.
	 *
	 * @return la cuenta de ejemplo
	 */
	private static Usuario cuentaDeEjemplo() {
		Usuario cuenta = new Usuario("1012345678", "Ana Pérez", "ana@correo.com", "x",
				Rol.ESTUDIANTE, EstadoCuenta.ACTIVO);
		ReflectionTestUtils.setField(cuenta, "id", 7L);
		return cuenta;
	}

	/** Un token emitido se valida y devuelve el correo, el rol, el jti y el vencimiento a 60 minutos. */
	@Test
	void unTokenEmitidoSeValida() {
		JwtService.TokenEmitido emitido = servicio.emitir(usuario);

		UsuarioAutenticado leido = servicio.validar(emitido.token()).orElseThrow();
		assertThat(leido.id()).isEqualTo(7L);
		assertThat(leido.correo()).isEqualTo("ana@correo.com");
		assertThat(leido.rol()).isEqualTo(Rol.ESTUDIANTE);
		assertThat(leido.jti()).isEqualTo(emitido.jti());
		assertThat(leido.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(60)));
		assertThat(emitido.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(60)));
	}

	/** Dos tokens del mismo usuario tienen jti distinto. */
	@Test
	void cadaTokenTieneUnJtiUnico() {
		assertThat(servicio.emitir(usuario).jti()).isNotEqualTo(servicio.emitir(usuario).jti());
	}

	/** Pasados los 60 minutos el token deja de ser válido. */
	@Test
	void unTokenVencidoSeRechaza() {
		String token = servicio.emitir(usuario).token();
		JwtService masTarde = new JwtService(PropiedadesDePrueba.estandar(),
				Clock.fixed(AHORA.plus(Duration.ofMinutes(61)), ZoneOffset.UTC));

		assertThat(masTarde.validar(token)).isEmpty();
		assertThat(new JwtService(PropiedadesDePrueba.estandar(),
				Clock.fixed(AHORA.plus(Duration.ofMinutes(59)), ZoneOffset.UTC)).validar(token)).isPresent();
	}

	/** Un token firmado con otro secreto no se acepta. */
	@Test
	void unTokenConOtraFirmaSeRechaza() {
		JwtService ajeno = new JwtService(PropiedadesDePrueba.con("otro-secreto-de-pruebas-de-32-caracteres!",
				PropiedadesDePrueba.LLAVE_AES, 60), Clock.fixed(AHORA, ZoneOffset.UTC));

		assertThat(servicio.validar(ajeno.emitir(usuario).token())).isEmpty();
	}

	/** Un token con el cuerpo alterado no se acepta. */
	@Test
	void unTokenAlteradoSeRechaza() {
		String[] partes = servicio.emitir(usuario).token().split("\\.");
		String cuerpo = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
				.replace("ESTUDIANTE", "ADMINISTRADOR");
		String alterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding()
				.encodeToString(cuerpo.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];

		assertThat(servicio.validar(alterado)).isEmpty();
	}

	/** Un token sin firma (algoritmo "none") no se acepta. */
	@Test
	void unTokenSinFirmaSeRechaza() {
		String sinFirma = Jwts.builder().subject("ana@correo.com").id("x").claim("rol", "ADMINISTRADOR")
				.claim("uid", 1).expiration(Date.from(AHORA.plusSeconds(600))).compact();

		assertThat(servicio.validar(sinFirma)).isEmpty();
	}

	/** Un token bien firmado pero sin los claims esperados no se acepta. */
	@Test
	void unTokenSinClaimsObligatoriosSeRechaza() {
		String sinRol = Jwts.builder().subject("ana@correo.com").id("x").claim("uid", 1)
				.expiration(Date.from(AHORA.plusSeconds(600)))
				.signWith(Keys.hmacShaKeyFor(PropiedadesDePrueba.SECRETO_JWT.getBytes(StandardCharsets.UTF_8)),
						Jwts.SIG.HS256)
				.compact();

		assertThat(servicio.validar(sinRol)).isEmpty();
	}

	/** Texto cualquiera, vacío o malformado no se acepta ni lanza error. */
	@Test
	void textoQueNoEsUnTokenSeRechaza() {
		assertThat(servicio.validar("")).isEmpty();
		assertThat(servicio.validar("abc.def.ghi")).isEmpty();
		assertThat(servicio.validar("no-es-un-token")).isEmpty();
	}
}
