package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.GrupoDto;
import co.edu.unbosque.arbolado.dto.GrupoRequest;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.GrupoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Grupos: administración para el administrador, oferta para el estudiante y grupos asignados al profesor. */
@RestController
@RequestMapping("/api")
@Tag(name = "Grupos")
public class GrupoController {

	/** Servicio de grupos. */
	private final GrupoService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de grupos
	 */
	public GrupoController(GrupoService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Lista los grupos.
	 *
	 * @param periodoId periodo a filtrar; sin él, todos
	 * @return los grupos con cupos
	 */
	@GetMapping("/admin/grupos")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@Operation(summary = "Listar grupos")
	public List<GrupoDto> listar(@RequestParam(required = false) Long periodoId) {
		return servicio.listar(periodoId);
	}

	/**
	 * Crea un grupo.
	 *
	 * @param solicitud datos del grupo
	 * @param admin     administrador autenticado
	 * @return el grupo creado
	 */
	@PostMapping("/admin/grupos")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear un grupo")
	public GrupoDto crear(@Valid @RequestBody GrupoRequest solicitud, @AuthenticationPrincipal UsuarioAutenticado admin) {
		return servicio.crear(solicitud, admin);
	}

	/**
	 * Consulta un grupo.
	 *
	 * @param id identificador del grupo
	 * @return el grupo
	 */
	@GetMapping("/admin/grupos/{id}")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@Operation(summary = "Consultar un grupo")
	public GrupoDto obtener(@PathVariable Long id) {
		return servicio.obtener(id);
	}

	/**
	 * Edita un grupo.
	 *
	 * @param id        identificador del grupo
	 * @param solicitud datos nuevos
	 * @param admin     administrador autenticado
	 * @return el grupo editado
	 */
	@PutMapping("/admin/grupos/{id}")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	@Operation(summary = "Editar un grupo")
	public GrupoDto editar(@PathVariable Long id, @Valid @RequestBody GrupoRequest solicitud,
						   @AuthenticationPrincipal UsuarioAutenticado admin) {
		return servicio.editar(id, solicitud, admin);
	}

	/**
	 * Oferta del periodo activo con cupos disponibles.
	 *
	 * @return los grupos del periodo activo
	 */
	@GetMapping("/grupos/oferta")
	@PreAuthorize("hasRole('ESTUDIANTE')")
	@Operation(summary = "Oferta de grupos del periodo activo")
	public List<GrupoDto> oferta() {
		return servicio.oferta();
	}

	/**
	 * Grupos del profesor autenticado.
	 *
	 * @param periodoId periodo a filtrar; sin él, todos
	 * @param profesor  profesor autenticado
	 * @return sus grupos
	 */
	@GetMapping("/profesor/grupos")
	@PreAuthorize("hasRole('PROFESOR')")
	@Operation(summary = "Mis grupos")
	public List<GrupoDto> misGrupos(@RequestParam(required = false) Long periodoId,
									@AuthenticationPrincipal UsuarioAutenticado profesor) {
		return servicio.delProfesor(profesor.id(), periodoId);
	}
}
