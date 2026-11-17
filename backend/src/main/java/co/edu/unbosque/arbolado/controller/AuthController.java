package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.LoginRequest;
import co.edu.unbosque.arbolado.dto.LoginResponse;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.AuthService;
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

/** Inicio y cierre de sesión. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

	/** Servicio de autenticación. */
	private final AuthService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de autenticación
	 */
	public AuthController(AuthService servicio) {
		this.servicio = servicio;
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
