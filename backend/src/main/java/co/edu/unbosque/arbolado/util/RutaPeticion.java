package co.edu.unbosque.arbolado.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilidad para obtener la ruta de una petición tal como la define el contrato (sin el contexto de
 * despliegue).
 */
public final class RutaPeticion {

	/** Clase de utilidad: no se instancia. */
	private RutaPeticion() {
	}

	/**
	 * Devuelve la ruta solicitada sin el contexto del WAR. En Tomcat, {@code /uni-arbolado-back/api/x}
	 * se informa como {@code /api/x}, igual que en el contrato.
	 *
	 * @param peticion petición HTTP actual
	 * @return la ruta sin contexto
	 */
	public static String de(HttpServletRequest peticion) {
		String uri = peticion.getRequestURI();
		String contexto = peticion.getContextPath();
		if (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)) {
			return uri.substring(contexto.length());
		}
		return uri;
	}
}
