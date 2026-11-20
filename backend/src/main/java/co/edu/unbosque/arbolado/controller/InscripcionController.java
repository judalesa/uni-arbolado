package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.dto.InscripcionRequest;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.InscripcionService;
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

/** Inscripción de asignaturas por el estudiante. */
@RestController
@RequestMapping("/api/inscripciones")
@Tag(name = "Inscripciones")
public class InscripcionController {

	/** Servicio de inscripciones. */
	private final InscripcionService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de inscripciones
	 */
	public InscripcionController(InscripcionService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Inscribe al estudiante autenticado en un grupo. El estudiante sale del token: nadie puede inscribir
	 * a otra persona.
	 *
	 * @param solicitud grupo elegido
	 * @param estudiante estudiante autenticado
	 * @return la inscripción creada
	 */
	@PostMapping
	@PreAuthorize("hasRole('ESTUDIANTE')")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Inscribir una asignatura")
	public InscripcionDto inscribir(@Valid @RequestBody InscripcionRequest solicitud,
									@AuthenticationPrincipal UsuarioAutenticado estudiante) {
		return servicio.inscribir(solicitud, estudiante);
	}
}
