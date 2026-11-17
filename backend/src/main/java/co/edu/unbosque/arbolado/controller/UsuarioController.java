package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.UsuarioDto;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil de quien hace la petición, para cualquier rol autenticado. */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios")
@PreAuthorize("isAuthenticated()")
public class UsuarioController {

	/** Servicio de usuarios. */
	private final UsuarioService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de usuarios
	 */
	public UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Consulta el perfil propio; la cuenta sale del token, no de un parámetro.
	 *
	 * @param actual identidad tomada del token
	 * @return los datos de la cuenta
	 */
	@GetMapping("/me")
	@Operation(summary = "Consultar mi perfil")
	public UsuarioDto miPerfil(@AuthenticationPrincipal UsuarioAutenticado actual) {
		return servicio.perfil(actual);
	}
}
