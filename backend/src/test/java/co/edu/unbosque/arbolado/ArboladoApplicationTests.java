package co.edu.unbosque.arbolado;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprueba que toda la aplicación arranca con la configuración de pruebas (H2, sin secretos reales). */
@SpringBootTest
@ActiveProfiles("test")
class ArboladoApplicationTests {

	/** Configuración tipada de la aplicación, inyectada desde el contexto levantado. */
	@Autowired
	private PropiedadesApp propiedades;

	/** Con el perfil de pruebas, el contexto completo arranca y la configuración tipada ({@link PropiedadesApp}) se carga con los valores esperados. */
	@Test
	void elContextoArrancaYLaConfiguracionTipadaSeCarga() {
		assertThat(propiedades.seguridad().jwtExpiracionMinutos()).isEqualTo(60);
		assertThat(propiedades.cors().allowedOrigins()).contains("http://localhost:4200");
		assertThat(propiedades.correo().modo()).isEqualTo(PropiedadesApp.ModoCorreo.LOG);
	}
}
