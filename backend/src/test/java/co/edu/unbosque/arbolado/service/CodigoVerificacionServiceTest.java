package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.CodigoVerificacion;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.CodigoVerificacionRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.PropiedadesDePrueba;
import co.edu.unbosque.arbolado.service.CodigoVerificacionService.Resultado;
import co.edu.unbosque.arbolado.service.CodigoVerificacionService.Veredicto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de las reglas del código: vigencia de 15 minutos, 5 intentos, un solo uso y reenvío. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CodigoVerificacionServiceTest {

	/** Reloj que las pruebas pueden adelantar. */
	private static final class RelojMutable extends Clock {

		/** Instante actual del reloj. */
		private Instant ahora = Instant.parse("2026-11-17T15:00:00Z");

		/**
		 * Adelanta el reloj.
		 *
		 * @param tiempo cuánto avanzar
		 */
		void avanzar(Duration tiempo) {
			ahora = ahora.plus(tiempo);
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zona) {
			return this;
		}

		@Override
		public Instant instant() {
			return ahora;
		}
	}

	/** Repositorio de códigos. */
	@Autowired
	private CodigoVerificacionRepository codigos;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Reloj de la prueba. */
	private RelojMutable reloj;

	/** Servicio bajo prueba, con el reloj controlable. */
	private CodigoVerificacionService servicio;

	/** Cuenta destinataria. */
	private Usuario usuario;

	/** Crea el servicio con el reloj de la prueba y una cuenta pendiente. */
	@BeforeEach
	void preparar() {
		reloj = new RelojMutable();
		servicio = new CodigoVerificacionService(codigos, PropiedadesDePrueba.estandar(), reloj);
		usuario = usuarios.save(new Usuario("8000000001", "Ana Prueba", "ana.codigos@example.com", "x",
				Rol.ESTUDIANTE, EstadoCuenta.PENDIENTE));
	}

	/**
	 * Devuelve un código de 6 dígitos distinto del indicado.
	 *
	 * @param codigo código correcto
	 * @return un código incorrecto
	 */
	private static String otroCodigo(String codigo) {
		return codigo.equals("000000") ? "000001" : "000000";
	}

	/** El código tiene 6 dígitos y en la base solo queda su HMAC de 64 caracteres, nunca el código. */
	@Test
	void guardaSoloLaHuellaHmac() {
		String codigo = servicio.generar(usuario);

		assertThat(codigo).matches("^[0-9]{6}$");
		CodigoVerificacion guardado = codigos.findFirstByUsuarioOrderByIdDesc(usuario).orElseThrow();
		assertThat(guardado.getCodigoHmac()).matches("^[0-9a-f]{64}$").isNotEqualTo(codigo);
		assertThat(guardado.getFechaExpiracion()).isEqualTo(reloj.instant().plus(Duration.ofMinutes(15)));
	}

	/** Un código correcto activa y queda usado: el segundo intento con el mismo código ya no sirve. */
	@Test
	void unCodigoSoloSeUsaUnaVez() {
		String codigo = servicio.generar(usuario);

		assertThat(servicio.validar(usuario, codigo).resultado()).isEqualTo(Resultado.CORRECTO);
		assertThat(servicio.validar(usuario, codigo).resultado()).isEqualTo(Resultado.NO_VIGENTE);
	}

	/** Vence a los 15 minutos: un segundo antes sirve y al cumplirse el límite ya no. */
	@Test
	void venceALos15Minutos() {
		String codigo = servicio.generar(usuario);
		reloj.avanzar(Duration.ofMinutes(15).minusSeconds(1));
		assertThat(servicio.validar(usuario, otroCodigo(codigo)).resultado()).isEqualTo(Resultado.INCORRECTO);

		reloj.avanzar(Duration.ofSeconds(1));
		assertThat(servicio.validar(usuario, codigo).resultado()).isEqualTo(Resultado.NO_VIGENTE);
	}

	/** Cuatro fallos informan los intentos que quedan; el quinto agota el código aunque luego llegue el correcto. */
	@Test
	void seInvalidaTrasCincoIntentosFallidos() {
		String codigo = servicio.generar(usuario);
		String malo = otroCodigo(codigo);

		for (int esperados = 4; esperados >= 1; esperados--) {
			Veredicto fallo = servicio.validar(usuario, malo);
			assertThat(fallo.resultado()).isEqualTo(Resultado.INCORRECTO);
			assertThat(fallo.intentosRestantes()).isEqualTo(esperados);
		}
		assertThat(servicio.validar(usuario, malo).resultado()).isEqualTo(Resultado.AGOTADO);
		assertThat(servicio.validar(usuario, codigo).resultado()).isEqualTo(Resultado.NO_VIGENTE);
	}

	/** Un código nuevo invalida el anterior: solo sirve el último. */
	@Test
	void elCodigoNuevoInvalidaAlAnterior() {
		String primero = servicio.generar(usuario);
		String segundo = servicio.generar(usuario);

		List<CodigoVerificacion> pendientes = codigos.findByUsuarioAndUsadoFalseAndInvalidadoFalse(usuario);
		assertThat(pendientes).hasSize(1);
		if (!primero.equals(segundo)) {
			assertThat(servicio.validar(usuario, primero).resultado()).isEqualTo(Resultado.INCORRECTO);
		}
		assertThat(servicio.validar(usuario, segundo).resultado()).isEqualTo(Resultado.CORRECTO);
	}

	/** Sin ningún código enviado no hay nada que validar. */
	@Test
	void sinCodigoNoHayNadaQueValidar() {
		assertThat(servicio.validar(usuario, "123456").resultado()).isEqualTo(Resultado.NO_VIGENTE);
	}

	/** Entre dos envíos deben pasar 60 segundos; se informa cuántos faltan. */
	@Test
	void exigeEsperarSesentaSegundosEntreEnvios() {
		assertThat(servicio.segundosDeEspera(usuario)).isEmpty();

		servicio.generar(usuario);
		reloj.avanzar(Duration.ofSeconds(45));
		assertThat(servicio.segundosDeEspera(usuario)).contains(15L);

		reloj.avanzar(Duration.ofSeconds(15));
		assertThat(servicio.segundosDeEspera(usuario)).isEmpty();
	}
}
