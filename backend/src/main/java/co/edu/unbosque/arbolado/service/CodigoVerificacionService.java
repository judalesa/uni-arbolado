package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import co.edu.unbosque.arbolado.model.CodigoVerificacion;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.CodigoVerificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Genera y valida los códigos de verificación de correo (RF-04, RF-05, RN-03): 6 dígitos, vencen a los
 * 15 minutos, un solo uso, 5 intentos y solo se guarda su HMAC.
 */
@Service
public class CodigoVerificacionService {

	/** Vigencia del código. */
	static final Duration VIGENCIA = Duration.ofMinutes(15);

	/** Espera mínima entre dos envíos a la misma cuenta (propuesta P-02). */
	static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofSeconds(60);

	/** Resultado de comparar un código recibido con el vigente. */
	public enum Resultado {
		/** El código coincide; queda usado. */
		CORRECTO,
		/** No coincide; todavía quedan intentos. */
		INCORRECTO,
		/** No coincide y se agotaron los intentos: el código ya no sirve. */
		AGOTADO,
		/** No hay un código utilizable: venció, se usó, se reemplazó o se agotó antes. */
		NO_VIGENTE
	}

	/**
	 * Veredicto de una validación.
	 *
	 * @param resultado          resultado de la comparación
	 * @param intentosRestantes  intentos que quedan después de esta comparación
	 */
	public record Veredicto(Resultado resultado, int intentosRestantes) {
	}

	/** Repositorio de códigos. */
	private final CodigoVerificacionRepository codigos;

	/** Llave HMAC derivada del secreto configurado. */
	private final SecretKeySpec llaveHmac;

	/** Reloj, inyectable para probar el vencimiento sin esperar. */
	private final Clock reloj;

	/** Fuente de azar criptográfica para generar los dígitos. */
	private final SecureRandom aleatorio = new SecureRandom();

	/**
	 * Crea el servicio con el reloj del sistema.
	 *
	 * @param codigos     repositorio de códigos
	 * @param propiedades configuración de la aplicación
	 */
	@Autowired
	public CodigoVerificacionService(CodigoVerificacionRepository codigos, PropiedadesApp propiedades) {
		this(codigos, propiedades, Clock.systemUTC());
	}

	/**
	 * Crea el servicio con un reloj propio.
	 *
	 * @param codigos     repositorio de códigos
	 * @param propiedades configuración de la aplicación
	 * @param reloj       fuente de la hora actual
	 */
	CodigoVerificacionService(CodigoVerificacionRepository codigos, PropiedadesApp propiedades, Clock reloj) {
		this.codigos = codigos;
		this.llaveHmac = new SecretKeySpec(
				propiedades.seguridad().codigoHmacSecreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		this.reloj = reloj;
	}

	/**
	 * Crea un código nuevo e invalida los anteriores de la cuenta (RF-05).
	 *
	 * @param usuario cuenta destinataria
	 * @return el código de 6 dígitos en claro, solo para enviarlo por correo; no se guarda
	 */
	@Transactional
	public String generar(Usuario usuario) {
		codigos.findByUsuarioAndUsadoFalseAndInvalidadoFalse(usuario).forEach(CodigoVerificacion::invalidar);
		String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
		Instant ahora = reloj.instant();
		codigos.save(new CodigoVerificacion(usuario, huella(codigo), ahora, ahora.plus(VIGENCIA)));
		return codigo;
	}

	/**
	 * Compara el código recibido con el vigente de la cuenta: cuenta los fallos y, al acertar, lo deja usado.
	 * Quien lo llama no debe revertir la transacción al rechazar, o los fallos no se conservarían.
	 *
	 * @param usuario cuenta que verifica
	 * @param codigo  código de 6 dígitos recibido
	 * @return el resultado y los intentos que quedan
	 */
	@Transactional
	public Veredicto validar(Usuario usuario, String codigo) {
		Optional<CodigoVerificacion> actual =
				codigos.findFirstByUsuarioAndUsadoFalseAndInvalidadoFalseOrderByIdDesc(usuario);
		if (actual.isEmpty() || !actual.get().estaVigente(reloj.instant())) {
			return new Veredicto(Resultado.NO_VIGENTE, 0);
		}
		CodigoVerificacion vigente = actual.get();
		if (iguales(vigente.getCodigoHmac(), huella(codigo))) {
			vigente.marcarUsado();
			return new Veredicto(Resultado.CORRECTO, CodigoVerificacion.MAX_INTENTOS - vigente.getIntentosFallidos());
		}
		vigente.registrarFallo();
		int restantes = CodigoVerificacion.MAX_INTENTOS - vigente.getIntentosFallidos();
		return new Veredicto(restantes > 0 ? Resultado.INCORRECTO : Resultado.AGOTADO, Math.max(restantes, 0));
	}

	/**
	 * Segundos que faltan para poder enviar otro código a la cuenta.
	 *
	 * @param usuario cuenta destinataria
	 * @return los segundos de espera, o vacío si ya se puede enviar
	 */
	@Transactional(readOnly = true)
	public Optional<Long> segundosDeEspera(Usuario usuario) {
		return codigos.findFirstByUsuarioOrderByIdDesc(usuario)
				.map(ultimo -> Duration.between(reloj.instant(), ultimo.getFechaCreacion().plus(ESPERA_ENTRE_ENVIOS)))
				.filter(restante -> !restante.isNegative() && !restante.isZero())
				.map(restante -> (restante.toMillis() + 999) / 1000);
	}

	/**
	 * Calcula el HMAC-SHA256 del código en hexadecimal.
	 *
	 * @param codigo código en claro
	 * @return 64 caracteres hexadecimales
	 */
	private String huella(String codigo) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(llaveHmac);
			return HexFormat.of().formatHex(mac.doFinal(codigo.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo calcular el HMAC del código", e);
		}
	}

	/**
	 * Compara dos huellas en tiempo constante para no filtrar información por el tiempo de respuesta.
	 *
	 * @param a primera huella
	 * @param b segunda huella
	 * @return {@code true} si son iguales
	 */
	private static boolean iguales(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}
}
