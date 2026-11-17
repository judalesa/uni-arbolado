package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.LoginRequest;
import co.edu.unbosque.arbolado.dto.LoginResponse;
import co.edu.unbosque.arbolado.dto.MensajeResponse;
import co.edu.unbosque.arbolado.dto.ReenviarRequest;
import co.edu.unbosque.arbolado.dto.RegistroRequest;
import co.edu.unbosque.arbolado.dto.VerificarRequest;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.AuthService;
import co.edu.unbosque.arbolado.service.RegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro, verificación de correo, inicio y cierre de sesión. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

	/** Servicio de autenticación. */
	private final AuthService servicio;

	/** Servicio de registro y verificación. */
	private final RegistroService registro;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de autenticación
	 * @param registro servicio de registro y verificación
	 */
	public AuthController(AuthService servicio, RegistroService registro) {
		this.servicio = servicio;
		this.registro = registro;
	}

	/**
	 * Registra a un estudiante y le envía el código de verificación (público).
	 *
	 * @param solicitud datos del registro
	 * @return mensaje de confirmación
	 */
	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrar un estudiante")
	public MensajeResponse registrar(@Valid @RequestBody RegistroRequest solicitud) {
		return registro.registrar(solicitud);
	}

	/**
	 * Verifica el correo con el código y activa la cuenta (público).
	 *
	 * @param solicitud correo y código
	 * @return mensaje de confirmación
	 */
	@PostMapping("/verificar")
	@Operation(summary = "Verificar el correo con el código")
	public MensajeResponse verificar(@Valid @RequestBody VerificarRequest solicitud) {
		return registro.verificar(solicitud);
	}

	/**
	 * Envía un código nuevo que invalida el anterior (público).
	 *
	 * @param solicitud correo de la cuenta
	 * @return mensaje de confirmación
	 */
	@PostMapping("/reenviar-codigo")
	@Operation(summary = "Reenviar el código de verificación")
	public MensajeResponse reenviar(@Valid @RequestBody ReenviarRequest solicitud) {
		return registro.reenviar(solicitud);
	}

	/**
	 * Inicia sesión (público).
	 *
	 * @param solicitud correo y contraseña
	 * @return el token y los datos de la cuenta
	 */
	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión")
	public LoginResponse login(@Valid @RequestBody LoginRequest solicitud) {
		return servicio.login(solicitud);
	}

	/**
	 * Cierra la sesión del token actual (cualquier rol autenticado).
	 *
	 * @param actual identidad tomada del token
	 */
	@PostMapping("/logout")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Cerrar sesión")
	public void logout(@AuthenticationPrincipal UsuarioAutenticado actual) {
		servicio.logout(actual);
	}
}
