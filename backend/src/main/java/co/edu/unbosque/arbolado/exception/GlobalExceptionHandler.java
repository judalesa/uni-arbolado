package co.edu.unbosque.arbolado.exception;

import co.edu.unbosque.arbolado.dto.ErrorCampo;
import co.edu.unbosque.arbolado.dto.ErrorResponse;
import co.edu.unbosque.arbolado.util.RutaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduce TODA excepción de la aplicación al formato {@link ErrorResponse} del contrato, con un código
 * HTTP estándar y un {@link CodigoError} estable.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/** Registro de la aplicación: aquí queda la traza completa de los errores inesperados. */
	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Excepciones de negocio propias ({@link ApiException}).
	 *
	 * @param ex       excepción lanzada por un servicio
	 * @param peticion petición HTTP en curso
	 * @return la respuesta con el estado y código de la excepción
	 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> manejarApi(ApiException ex, HttpServletRequest peticion) {
		ErrorResponse cuerpo = ErrorResponse.de(ex.getCodigo(), ex.getMessage(), RutaPeticion.de(peticion));
		if (ex instanceof ReglaNegocioException regla && !regla.getMotivos().isEmpty()) {
			cuerpo = cuerpo.conMotivos(regla.getMotivos());
		}
		ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(ex.getCodigo().estado());
		if (ex instanceof DemasiadosIntentosException demasiados) {
			respuesta.header(HttpHeaders.RETRY_AFTER, String.valueOf(demasiados.getSegundosEspera()));
		}
		return respuesta.body(cuerpo);
	}

	/**
	 * Validación de parámetros o variables de ruta anotados con restricciones (por ejemplo
	 * {@code @Positive} en un {@code @PathVariable}) cuando falla dentro de un servicio.
	 *
	 * @param ex       violaciones detectadas
	 * @param peticion petición HTTP en curso
	 * @return respuesta 400 con un detalle por campo
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> manejarRestricciones(ConstraintViolationException ex,
															 HttpServletRequest peticion) {
		List<ErrorCampo> detalles = ex.getConstraintViolations().stream()
				.map(v -> {
					String ruta = v.getPropertyPath().toString();
					return new ErrorCampo(ruta.substring(ruta.lastIndexOf('.') + 1), v.getMessage());
				})
				.toList();
		return validacion("Los datos enviados no son válidos", detalles, RutaPeticion.de(peticion));
	}

	/**
	 * Un parámetro de la URL tiene un tipo incorrecto (por ejemplo texto donde se esperaba un número).
	 *
	 * @param ex       error de conversión
	 * @param peticion petición HTTP en curso
	 * @return respuesta 400 que indica el parámetro
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex,
															  HttpServletRequest peticion) {
		return validacion("Los datos enviados no son válidos",
				List.of(new ErrorCampo(ex.getName(), "El valor tiene un formato incorrecto")),
				RutaPeticion.de(peticion));
	}

	/**
	 * Un servicio protegido con seguridad por método rechazó al usuario autenticado.
	 *
	 * @param ex       excepción de Spring Security
	 * @param peticion petición HTTP en curso
	 * @return respuesta 403 {@code ACCESO_DENEGADO}
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest peticion) {
		return construir(CodigoError.ACCESO_DENEGADO, "No tienes permiso para realizar esta acción",
				RutaPeticion.de(peticion));
	}

	/**
	 * Fallo de autenticación detectado dentro de un controlador o servicio.
	 *
	 * @param ex       excepción de Spring Security
	 * @param peticion petición HTTP en curso
	 * @return respuesta 401 {@code NO_AUTENTICADO}
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> manejarAutenticacion(AuthenticationException ex, HttpServletRequest peticion) {
		return construir(CodigoError.NO_AUTENTICADO, "Debes iniciar sesión para continuar", RutaPeticion.de(peticion));
	}

	/**
	 * Red de seguridad: cualquier error no previsto. Responde con un mensaje genérico y deja la traza
	 * completa solo en el registro del servidor.
	 *
	 * @param ex       error inesperado
	 * @param peticion petición HTTP en curso
	 * @return respuesta 500 {@code ERROR_INTERNO}
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> manejarInesperado(Exception ex, HttpServletRequest peticion) {
		LOG.error("Error inesperado en {} {}", peticion.getMethod(), RutaPeticion.de(peticion), ex);
		return construir(CodigoError.ERROR_INTERNO, "Ocurrió un error inesperado. Intenta de nuevo más tarde",
				RutaPeticion.de(peticion));
	}

	// ----- Excepciones propias de Spring MVC: se reutiliza la clase base y se cambia el cuerpo -----

	/** {@inheritDoc} Un cuerpo {@code @Valid} inválido se informa campo por campo. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
																  HttpHeaders headers, HttpStatusCode status,
																  WebRequest request) {
		List<ErrorCampo> detalles = new ArrayList<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(e -> detalles.add(new ErrorCampo(e.getField(), e.getDefaultMessage())));
		ex.getBindingResult().getGlobalErrors()
				.forEach(e -> detalles.add(new ErrorCampo(e.getObjectName(), e.getDefaultMessage())));
		return respuestaObjeto(validacion("Los datos enviados no son válidos", detalles, ruta(request)));
	}

	/** {@inheritDoc} Validación de parámetros de método (RequestParam, PathVariable) con restricciones. */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
																			HttpHeaders headers, HttpStatusCode status,
																			WebRequest request) {
		List<ErrorCampo> detalles = new ArrayList<>();
		ex.getParameterValidationResults().forEach(resultado -> {
			String campo = resultado.getMethodParameter().getParameterName();
			resultado.getResolvableErrors().forEach(error ->
					detalles.add(new ErrorCampo(campo != null ? campo : "parametro", error.getDefaultMessage())));
		});
		return respuestaObjeto(validacion("Los datos enviados no son válidos", detalles, ruta(request)));
	}

	/** {@inheritDoc} JSON mal formado o con tipos incorrectos. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
																  HttpHeaders headers, HttpStatusCode status,
																  WebRequest request) {
		return respuestaObjeto(validacion("El cuerpo de la petición no es un JSON válido", List.of(), ruta(request)));
	}

	/**
	 * Punto único por el que pasan las demás excepciones de Spring MVC (404, 405, 406, 415, parámetro
	 * obligatorio ausente…). Se les asigna un {@link CodigoError} según el estado HTTP.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
															 HttpStatusCode statusCode, WebRequest request) {
		CodigoError codigo = codigoPara(statusCode.value());
		String mensaje = switch (codigo) {
			case NO_ENCONTRADO -> "El recurso solicitado no existe";
			case METODO_NO_PERMITIDO -> "Esa operación no está permitida en esta ruta";
			case NO_ACEPTABLE -> "La API solo responde en formato JSON";
			case TIPO_NO_SOPORTADO -> "El cuerpo de la petición debe enviarse como application/json";
			case ERROR_INTERNO -> "Ocurrió un error inesperado. Intenta de nuevo más tarde";
			default -> "La petición no es válida";
		};
		if (codigo == CodigoError.ERROR_INTERNO) {
			LOG.error("Error interno de Spring MVC en {}", ruta(request), ex);
		}
		ErrorResponse cuerpo = ErrorResponse.de(codigo, mensaje, ruta(request));
		return ResponseEntity.status(statusCode).headers(headers).body(cuerpo);
	}

	// ----- Utilidades privadas -----

	/**
	 * Asigna un {@link CodigoError} a un estado HTTP producido por Spring MVC.
	 *
	 * @param estado estado HTTP
	 * @return el código estable equivalente
	 */
	private static CodigoError codigoPara(int estado) {
		return switch (estado) {
			case 404 -> CodigoError.NO_ENCONTRADO;
			case 405 -> CodigoError.METODO_NO_PERMITIDO;
			case 406 -> CodigoError.NO_ACEPTABLE;
			case 415 -> CodigoError.TIPO_NO_SOPORTADO;
			default -> estado >= 500 ? CodigoError.ERROR_INTERNO : CodigoError.VALIDACION;
		};
	}

	/**
	 * Obtiene la ruta de la petición sin el contexto del WAR.
	 *
	 * @param request petición envuelta por Spring
	 * @return la ruta, o cadena vacía si no es una petición de servlet
	 */
	private static String ruta(WebRequest request) {
		return request instanceof ServletWebRequest servlet ? RutaPeticion.de(servlet.getRequest()) : "";
	}

	/**
	 * Construye una respuesta de error simple.
	 *
	 * @param codigo  código estable (define el estado HTTP)
	 * @param mensaje explicación legible
	 * @param ruta    ruta solicitada
	 * @return la respuesta lista para devolver
	 */
	private static ResponseEntity<ErrorResponse> construir(CodigoError codigo, String mensaje, String ruta) {
		return ResponseEntity.status(codigo.estado()).body(ErrorResponse.de(codigo, mensaje, ruta));
	}

	/**
	 * Construye una respuesta 400 {@code VALIDACION}, con detalles por campo cuando los hay.
	 *
	 * @param mensaje  explicación general
	 * @param detalles errores por campo (puede estar vacía)
	 * @param ruta     ruta solicitada
	 * @return la respuesta 400
	 */
	private static ResponseEntity<ErrorResponse> validacion(String mensaje, List<ErrorCampo> detalles, String ruta) {
		ErrorResponse cuerpo = ErrorResponse.de(CodigoError.VALIDACION, mensaje, ruta);
		if (!detalles.isEmpty()) {
			cuerpo = cuerpo.conDetalles(detalles);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
	}

	/**
	 * Adapta el tipo de la respuesta al que exige {@link ResponseEntityExceptionHandler}.
	 *
	 * @param respuesta respuesta ya construida
	 * @return la misma respuesta con cuerpo de tipo {@link Object}
	 */
	private static ResponseEntity<Object> respuestaObjeto(ResponseEntity<ErrorResponse> respuesta) {
		return ResponseEntity.status(respuesta.getStatusCode()).body(respuesta.getBody());
	}
}
