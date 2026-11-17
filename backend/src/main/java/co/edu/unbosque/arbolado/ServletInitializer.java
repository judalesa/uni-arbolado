package co.edu.unbosque.arbolado;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Permite que el empaquetado WAR arranque la aplicación dentro de un Tomcat externo
 * (el Web Application Manager del servidor del profesor).
 */
public class ServletInitializer extends SpringBootServletInitializer {

	/**
	 * Indica a Tomcat cuál es la clase principal de la aplicación.
	 *
	 * @param application constructor de la aplicación proporcionado por el contenedor
	 * @return el constructor configurado con {@link ArboladoApplication}
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(ArboladoApplication.class);
	}

}
