package co.edu.unbosque.arbolado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del backend del Sistema de Gestión Académica en Línea de la Universidad El Arbolado.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ArboladoApplication {

	/**
	 * Arranca la aplicación de forma independiente (desarrollo local con {@code ./mvnw spring-boot:run}).
	 *
	 * @param args argumentos de línea de comandos
	 */
	public static void main(String[] args) {
		SpringApplication.run(ArboladoApplication.class, args);
	}

}
