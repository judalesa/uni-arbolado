package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.repository.TokenRevocadoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de la lista de tokens revocados. */
@SpringBootTest
@ActiveProfiles("test")
class TokenRevocadoServiceTest {

	/** Servicio bajo prueba. */
	@Autowired
	private TokenRevocadoService servicio;

	/** Repositorio de tokens revocados. */
	@Autowired
	private TokenRevocadoRepository repositorio;

	/** Un token revocado se reconoce como tal y uno desconocido no. */
	@Test
	void unTokenRevocadoSeReconoce() {
		servicio.revocar("jti-revocado", Instant.now().plus(Duration.ofMinutes(30)));

		assertThat(servicio.estaRevocado("jti-revocado")).isTrue();
		assertThat(servicio.estaRevocado("jti-desconocido")).isFalse();
	}

	/** Revocar dos veces el mismo token no duplica el registro ni falla. */
	@Test
	void revocarDosVecesNoDuplica() {
		servicio.revocar("jti-doble", Instant.now().plus(Duration.ofMinutes(30)));
		long antes = repositorio.count();

		servicio.revocar("jti-doble", Instant.now().plus(Duration.ofMinutes(30)));

		assertThat(repositorio.count()).isEqualTo(antes);
	}

	/** Al revocar se purgan los registros de tokens que ya vencieron. */
	@Test
	void seBorranLosRegistrosVencidos() {
		servicio.revocar("jti-vencido", Instant.now().minus(Duration.ofMinutes(5)));
		assertThat(servicio.estaRevocado("jti-vencido")).isTrue();

		servicio.revocar("jti-vigente", Instant.now().plus(Duration.ofMinutes(30)));

		assertThat(servicio.estaRevocado("jti-vencido")).isFalse();
		assertThat(servicio.estaRevocado("jti-vigente")).isTrue();
	}
}
