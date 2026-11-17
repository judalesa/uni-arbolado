package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;

import java.util.List;

/** Fábrica de configuración con valores falsos para las pruebas unitarias de seguridad. */
final class PropiedadesDePrueba {

	/** Llave AES-256 falsa: 32 bytes en cero, codificados en Base64. */
	static final String LLAVE_AES = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

	/** Secreto JWT falso de más de 32 caracteres. */
	static final String SECRETO_JWT = "solo-para-pruebas-no-es-un-secreto-0000000000";

	/** Evita instanciar la fábrica. */
	private PropiedadesDePrueba() {
	}

	/**
	 * Construye una configuración completa con el secreto y la llave indicados.
	 *
	 * @param secretoJwt secreto de firma del JWT
	 * @param llaveAes   llave AES en Base64
	 * @param minutos    vigencia del token en minutos
	 * @return la configuración de prueba
	 */
	static PropiedadesApp con(String secretoJwt, String llaveAes, int minutos) {
		return new PropiedadesApp(
				new PropiedadesApp.Seguridad(secretoJwt, minutos, llaveAes, "hmac-de-pruebas"),
				new PropiedadesApp.Admin("0000000001", "Admin", "admin@example.com", "clave-de-pruebas-1"),
				new PropiedadesApp.Cors(List.of("http://localhost:4200")),
				new PropiedadesApp.Correo(PropiedadesApp.ModoCorreo.LOG, "pruebas@example.com"));
	}

	/**
	 * Configuración estándar de pruebas con vigencia de 60 minutos.
	 *
	 * @return la configuración de prueba
	 */
	static PropiedadesApp estandar() {
		return con(SECRETO_JWT, LLAVE_AES, 60);
	}
}
