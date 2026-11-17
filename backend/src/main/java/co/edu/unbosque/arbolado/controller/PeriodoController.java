package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.PeriodoDto;
import co.edu.unbosque.arbolado.dto.PeriodoRequest;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.PeriodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Periodos académicos: administración para el administrador y consulta del activo para cualquier rol. */
@RestController
@RequestMapping("/api/periodos")
@Tag(name = "Periodos")
public class PeriodoController {

	/** Servicio de periodos. */
	private final PeriodoService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de periodos
	 */
	public PeriodoController(PeriodoService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Lista los periodos.
	 *
	 * @return todos los periodos, del más reciente al más antiguo
	 */
	@GetMapping
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@Operation(summary = "Listar periodos")
	public List<PeriodoDto> listar() {
		return servicio.listar();
	}

	/**
	 * Crea un periodo inactivo.
	 *
	 * @param solicitud datos del periodo
	 * @param admin     administrador autenticado
	 * @return el periodo creado
	 */
	@PostMapping
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear un periodo")
	public PeriodoDto crear(@Valid @RequestBody PeriodoRequest solicitud,
							@AuthenticationPrincipal UsuarioAutenticado admin) {
		return servicio.crear(solicitud, admin);
	}

	/**
	 * Consulta el periodo activo (cualquier rol autenticado).
	 *
	 * @return el periodo activo
	 */
	@GetMapping("/activo")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Consultar el periodo activo")
	public PeriodoDto activo() {
		return servicio.activo();
	}

	/**
	 * Modifica código y fechas de un periodo.
	 *
	 * @param id        periodo a modificar
	 * @param solicitud datos nuevos
	 * @param admin     administrador autenticado
	 * @return el periodo modificado
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@Operation(summary = "Modificar un periodo")
	public PeriodoDto modificar(@PathVariable Long id, @Valid @RequestBody PeriodoRequest solicitud,
								@AuthenticationPrincipal UsuarioAutenticado admin) {
		return servicio.modificar(id, solicitud, admin);
	}

	/**
	 * Activa un periodo y desactiva el vigente.
	 *
	 * @param id    periodo a activar
	 * @param admin administrador autenticado
	 * @return el periodo activado
	 */
	@PostMapping("/{id}/activar")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@Operation(summary = "Activar un periodo")
	public PeriodoDto activar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado admin) {
		return servicio.activar(id, admin);
	}
}
