package co.edu.unbosque.arbolado.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del codificador AES-GCM sobre BCrypt (RN-06). */
class CodificadorContrasenaTest {

	/** Codificador bajo prueba. */
	private final CodificadorContrasena codificador = new CodificadorContrasena(PropiedadesDePrueba.estandar());

	/** Una clave codificada se reconoce con la misma clave y no con otra. */
	@Test
	void reconoceLaClaveCorrectaYRechazaLaIncorrecta() {
		String guardada = codificador.encode("MiClave123");

		assertThat(codificador.matches("MiClave123", guardada)).isTrue();
		assertThat(codificador.matches("miclave123", guardada)).isFalse();
	}

	/** El valor guardado no contiene la clave ni parece un hash BCrypt legible. */
	@Test
	void elValorGuardadoEstaCifrado() {
		String guardada = codificador.encode("MiClave123");

		assertThat(guardada).doesNotContain("MiClave123").doesNotStartWith("$2");
		assertThat(guardada.length()).isLessThanOrEqualTo(255);
	}

	/** Cada cifrado usa un IV distinto: la misma clave nunca produce el mismo valor. */
	@Test
	void cadaCodificacionUsaUnIvDistinto() {
		String primera = codificador.encode("MiClave123");
		String segunda = codificador.encode("MiClave123");

		byte[] ivPrimera = java.util.Arrays.copyOf(Base64.getDecoder().decode(primera), CodificadorContrasena.LONGITUD_IV);
		byte[] ivSegunda = java.util.Arrays.copyOf(Base64.getDecoder().decode(segunda), CodificadorContrasena.LONGITUD_IV);
		assertThat(primera).isNotEqualTo(segunda);
		assertThat(ivPrimera).isNotEqualTo(ivSegunda);
		assertThat(codificador.matches("MiClave123", segunda)).isTrue();
	}

	/** Un valor alterado se rechaza: GCM detecta la manipulación. */
	@Test
	void unValorManipuladoSeRechaza() {
		byte[] bytes = Base64.getDecoder().decode(codificador.encode("MiClave123"));
		bytes[bytes.length - 1] ^= 1;

		assertThat(codificador.matches("MiClave123", Base64.getEncoder().encodeToString(bytes))).isFalse();
	}

	/** Un valor guardado con otra llave no se puede descifrar. */
	@Test
	void unaLlaveDistintaNoDescifra() {
		byte[] bytes = new byte[32];
		java.util.Arrays.fill(bytes, (byte) 7);
		String otraLlave = Base64.getEncoder().encodeToString(bytes);
		CodificadorContrasena otro = new CodificadorContrasena(
				PropiedadesDePrueba.con(PropiedadesDePrueba.SECRETO_JWT, otraLlave, 60));

		assertThat(otro.matches("MiClave123", codificador.encode("MiClave123"))).isFalse();
	}

	/** Valores vacíos, nulos o que no son Base64 devuelven falso sin lanzar error. */
	@Test
	void losValoresInvalidosDevuelvenFalso() {
		assertThat(codificador.matches("x", null)).isFalse();
		assertThat(codificador.matches("x", "")).isFalse();
		assertThat(codificador.matches("x", "esto no es base64 !!")).isFalse();
		assertThat(codificador.matches("x", Base64.getEncoder().encodeToString(new byte[5]))).isFalse();
	}

	/** Una clave de más de 72 bytes no se acepta, porque BCrypt ignoraría el resto. */
	@Test
	void unaClaveDeMasDe72BytesSeRechaza() {
		String larga = "a".repeat(73);

		assertThatThrownBy(() -> codificador.encode(larga)).isInstanceOf(IllegalArgumentException.class);
		assertThat(codificador.matches(larga, codificador.encode("a".repeat(72)))).isFalse();
	}

	/** Una llave que no mide 32 bytes impide crear el codificador. */
	@Test
	void unaLlaveDeLongitudIncorrectaImpideArrancar() {
		String corta = Base64.getEncoder().encodeToString(new byte[16]);

		assertThatThrownBy(() -> new CodificadorContrasena(
				PropiedadesDePrueba.con(PropiedadesDePrueba.SECRETO_JWT, corta, 60)))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new CodificadorContrasena(
				PropiedadesDePrueba.con(PropiedadesDePrueba.SECRETO_JWT, "no es base64!", 60)))
				.isInstanceOf(IllegalStateException.class);
	}
}
