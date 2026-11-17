package co.edu.unbosque.arbolado.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica la regla "sin valores por defecto para secretos": si falta o es débil algún valor, la
 * aplicación no arranca.
 */
class PropiedadesAppTest {

	/** Conjunto mínimo de propiedades válidas; cada prueba lo altera para provocar un fallo concreto. */
	private static final String[] VALIDAS = {
			"app.seguridad.jwt-secreto=solo-para-pruebas-no-es-un-secreto-0000000000",
			"app.seguridad.jwt-expiracion-minutos=60",
			"app.seguridad.aes-llave-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
			"app.seguridad.codigo-hmac-secreto=hmac-de-prueba",
			"app.admin.documento=0000000001",
			"app.admin.nombre=Administrador",
			"app.admin.correo=admin@example.com",
			"app.admin.contrasena=clave-de-pruebas-1",
			"app.cors.allowed-origins=http://localhost:4200",
			"app.correo.modo=log",
			"app.correo.remitente=pruebas@example.com"
	};

	/** Levanta un contexto liviano de Spring solo con la clase de configuración tipada. */
	private final ApplicationContextRunner ejecutor = new ApplicationContextRunner()
			.withUserConfiguration(Habilitar.class);

	/** Configuración de prueba que activa la lectura de {@link PropiedadesApp}. */
	@EnableConfigurationProperties(PropiedadesApp.class)
	static class Habilitar {
	}

	/** Devuelve las propiedades válidas, quitando las que empiecen por {@code sinPrefijo}. */
	private static String[] sin(String sinPrefijo) {
		return java.util.Arrays.stream(VALIDAS).filter(p -> !p.startsWith(sinPrefijo)).toArray(String[]::new);
	}

	/** Devuelve las propiedades válidas reemplazando la que empieza por el prefijo dado. */
	private static String[] cambiando(String prefijo, String nueva) {
		return java.util.stream.Stream.concat(java.util.Arrays.stream(sin(prefijo)), java.util.stream.Stream.of(nueva))
				.toArray(String[]::new);
	}

	/** Con todos los valores válidos, la configuración se carga sin errores. */
	@Test
	void conTodosLosValoresValidosElContextoArranca() {
		ejecutor.withPropertyValues(VALIDAS).run(contexto -> {
			assertThat(contexto).hasNotFailed();
			assertThat(contexto.getBean(PropiedadesApp.class).admin().documento()).isEqualTo("0000000001");
		});
	}

	/** Si falta el secreto del JWT, el contexto falla (no hay valor por defecto). */
	@Test
	void sinSecretoJwtNoArranca() {
		ejecutor.withPropertyValues(sin("app.seguridad.jwt-secreto"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}

	/** Un secreto del JWT de menos de 32 caracteres se rechaza por débil. */
	@Test
	void conSecretoJwtCortoNoArranca() {
		ejecutor.withPropertyValues(cambiando("app.seguridad.jwt-secreto", "app.seguridad.jwt-secreto=corto"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}

	/** Si falta la llave AES de las contraseñas, el contexto falla. */
	@Test
	void sinLlaveAesNoArranca() {
		ejecutor.withPropertyValues(sin("app.seguridad.aes-llave-base64"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}

	/** Un documento de administrador que no tenga 10 dígitos se rechaza. */
	@Test
	void conDocumentoDeAdministradorInvalidoNoArranca() {
		ejecutor.withPropertyValues(cambiando("app.admin.documento", "app.admin.documento=123"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}

	/** Una contraseña de administrador de menos de 8 caracteres se rechaza. */
	@Test
	void conContrasenaDeAdministradorCortaNoArranca() {
		ejecutor.withPropertyValues(cambiando("app.admin.contrasena", "app.admin.contrasena=1234"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}

	/** Sin ningún origen CORS configurado, el contexto falla. */
	@Test
	void sinOrigenesCorsNoArranca() {
		ejecutor.withPropertyValues(sin("app.cors.allowed-origins"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}

	/** Un modo de correo que no sea LOG ni SMTP se rechaza. */
	@Test
	void conModoDeCorreoDesconocidoNoArranca() {
		ejecutor.withPropertyValues(cambiando("app.correo.modo", "app.correo.modo=paloma"))
				.run(contexto -> assertThat(contexto).hasFailed());
	}
}
