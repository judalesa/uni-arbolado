package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.dto.ErrorResponse;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.util.RutaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Responde con el formato {@link ErrorResponse} los rechazos que ocurren <b>dentro de los filtros</b> de
 * Spring Security, antes de llegar a cualquier controlador.
 */
@Component
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

	/** Serializador JSON de Spring Boot (Jackson 3) con el que se escribe el cuerpo del error. */
	private final JsonMapper mapper;

	/**
	 * Crea el manejador.
	 *
	 * @param mapper serializador JSON configurado por Spring Boot
	 */
	public ManejadorErroresSeguridad(JsonMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Petición sin autenticación válida: responde 401.
	 *
	 * @param request       petición rechazada
	 * @param response      respuesta a completar
	 * @param authException motivo del rechazo (no se expone al cliente)
	 * @throws IOException si falla la escritura de la respuesta
	 */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		escribir(response, ErrorResponse.de(CodigoError.NO_AUTENTICADO,
				"Debes iniciar sesión para continuar", RutaPeticion.de(request)));
	}

	/**
	 * Usuario autenticado sin permiso para el recurso: responde 403.
	 *
	 * @param request               petición rechazada
	 * @param response              respuesta a completar
	 * @param accessDeniedException motivo del rechazo (no se expone al cliente)
	 * @throws IOException si falla la escritura de la respuesta
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
					   AccessDeniedException accessDeniedException) throws IOException {
		escribir(response, ErrorResponse.de(CodigoError.ACCESO_DENEGADO,
				"No tienes permiso para realizar esta acción", RutaPeticion.de(request)));
	}

	/**
	 * Escribe el error como JSON en la respuesta, con su estado HTTP.
	 *
	 * @param response respuesta a completar
	 * @param cuerpo   error en el formato del contrato
	 * @throws IOException si falla la escritura
	 */
	private void escribir(HttpServletResponse response, ErrorResponse cuerpo) throws IOException {
		response.setStatus(cuerpo.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getOutputStream(), cuerpo);
	}
}
