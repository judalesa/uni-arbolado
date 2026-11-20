package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Codifica contraseñas como {@code Base64(IV ‖ AES-GCM(BCrypt(clave)))}.
 * BCrypt aporta el hash lento con sal; AES-GCM lo cifra con una llave que vive solo en la configuración
 * local, así un volcado de la base de datos no basta para atacar las claves. El IV es aleatorio y distinto
 * en cada cifrado: repetirlo con la misma llave rompe GCM.
 */
@Component
public class CodificadorContrasena implements PasswordEncoder {

	/** Longitud del IV recomendada para GCM, en bytes. */
	static final int LONGITUD_IV = 12;

	/** Longitud de la etiqueta de autenticación de GCM, en bits. */
	private static final int BITS_ETIQUETA = 128;

	/** BCrypt solo procesa los primeros 72 bytes de la clave. */
	private static final int MAX_BYTES_CLAVE = 72;

	/** Longitud de una llave AES-256, en bytes. */
	private static final int BYTES_LLAVE = 32;

	/** Generador de números aleatorios criptográficamente seguro, para los IV. */
	private final SecureRandom aleatorio = new SecureRandom();

	/** Codificador BCrypt con el costo por defecto. */
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	/** Llave AES de 256 bits. */
	private final SecretKeySpec llave;

	/**
	 * Crea el codificador con la llave de la configuración local.
	 *
	 * @param propiedades configuración de la aplicación
	 * @throws IllegalStateException si la llave no es Base64 válido de 32 bytes
	 */
	public CodificadorContrasena(PropiedadesApp propiedades) {
		this.llave = construirLlave(propiedades.seguridad().aesLlaveBase64());
	}

	/**
	 * Convierte la llave de Base64 a bytes y comprueba su longitud.
	 *
	 * @param base64 llave codificada
	 * @return llave AES lista para usar
	 */
	private static SecretKeySpec construirLlave(String base64) {
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(base64.trim());
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("app.seguridad.aes-llave-base64 no es Base64 válido");
		}
		if (bytes.length != BYTES_LLAVE) {
			throw new IllegalStateException("app.seguridad.aes-llave-base64 debe decodificar a 32 bytes (AES-256)");
		}
		return new SecretKeySpec(bytes, "AES");
	}

	/**
	 * Aplica BCrypt a la clave y cifra el hash con AES-GCM.
	 *
	 * @param clave contraseña en texto plano
	 * @return texto Base64 con el IV seguido del hash cifrado
	 * @throws IllegalArgumentException si la clave supera 72 bytes
	 */
	@Override
	public String encode(CharSequence clave) {
		if (excedeLimite(clave)) {
			throw new IllegalArgumentException("La contraseña supera los 72 bytes que admite BCrypt");
		}
		try {
			byte[] iv = new byte[LONGITUD_IV];
			aleatorio.nextBytes(iv);
			Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
			cifrador.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(BITS_ETIQUETA, iv));
			byte[] cifrado = cifrador.doFinal(bcrypt.encode(clave).getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
					.put(iv).put(cifrado).array());
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo cifrar la contraseña", e);
		}
	}

	/**
	 * Descifra el valor guardado y compara el hash BCrypt con la clave recibida.
	 *
	 * @param clave     contraseña en texto plano
	 * @param guardada  valor guardado en la base de datos
	 * @return {@code true} si coincide; {@code false} si no, o si el valor está corrupto o fue manipulado
	 */
	@Override
	public boolean matches(CharSequence clave, String guardada) {
		if (clave == null || guardada == null || excedeLimite(clave)) {
			return false;
		}
		try {
			byte[] todo = Base64.getDecoder().decode(guardada);
			if (todo.length <= LONGITUD_IV) {
				return false;
			}
			Cipher cifrador = Cipher.getInstance("AES/GCM/NoPadding");
			cifrador.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(BITS_ETIQUETA, todo, 0, LONGITUD_IV));
			byte[] hash = cifrador.doFinal(todo, LONGITUD_IV, todo.length - LONGITUD_IV);
			return bcrypt.matches(clave, new String(hash, StandardCharsets.UTF_8));
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Indica si la clave ocupa más bytes de los que BCrypt procesa.
	 *
	 * @param clave contraseña en texto plano
	 * @return {@code true} si supera 72 bytes en UTF-8
	 */
	private static boolean excedeLimite(CharSequence clave) {
		return clave.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CLAVE;
	}
}
