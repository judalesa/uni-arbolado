package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.CrearProfesorRequest;
import co.edu.unbosque.arbolado.dto.PaginaDto;
import co.edu.unbosque.arbolado.dto.UsuarioDto;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de profesores y consulta de estudiantes; exclusiva del administrador. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsuarioController {

	/** Servicio de usuarios. */
	private final UsuarioService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de usuarios
	 */
	public AdminUsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Crea un profesor activo, sin verificación de correo.
	 *
	 * @param solicitud datos del profesor
	 * @param admin     administrador autenticado
	 * @return el profesor creado
	 */
	@PostMapping("/profesores")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear un profesor")
	public UsuarioDto crearProfesor(@Valid @RequestBody CrearProfesorRequest solicitud,
									@AuthenticationPrincipal UsuarioAutenticado admin) {
		return servicio.crearProfesor(solicitud, admin);
	}

	/**
	 * Lista los profesores.
	 *
	 * @param q      texto a buscar en nombre, documento o correo
	 * @param pagina número de página, desde 0
	 * @param tamano elementos por página
	 * @return la página de profesores
	 */
	@GetMapping("/profesores")
	@Operation(summary = "Listar profesores")
	public PaginaDto<UsuarioDto> listarProfesores(@RequestParam(required = false) String q,
												  @RequestParam(defaultValue = "0") int pagina,
												  @RequestParam(defaultValue = "20") int tamano) {
		return servicio.listar(Rol.PROFESOR, q, pagina, tamano);
	}

	/**
	 * Consulta un profesor.
	 *
	 * @param id identificador de la cuenta
	 * @return el profesor
	 */
	@GetMapping("/profesores/{id}")
	@Operation(summary = "Consultar un profesor")
	public UsuarioDto obtenerProfesor(@PathVariable Long id) {
		return servicio.obtener(Rol.PROFESOR, id);
	}

	/**
	 * Lista los estudiantes.
	 *
	 * @param q      texto a buscar en nombre, documento o correo
	 * @param pagina número de página, desde 0
	 * @param tamano elementos por página
	 * @return la página de estudiantes
	 */
	@GetMapping("/estudiantes")
	@Operation(summary = "Listar estudiantes")
	public PaginaDto<UsuarioDto> listarEstudiantes(@RequestParam(required = false) String q,
												   @RequestParam(defaultValue = "0") int pagina,
												   @RequestParam(defaultValue = "20") int tamano) {
		return servicio.listar(Rol.ESTUDIANTE, q, pagina, tamano);
	}

	/**
	 * Consulta un estudiante.
	 *
	 * @param id identificador de la cuenta
	 * @return el estudiante
	 */
	@GetMapping("/estudiantes/{id}")
	@Operation(summary = "Consultar un estudiante")
	public UsuarioDto obtenerEstudiante(@PathVariable Long id) {
		return servicio.obtener(Rol.ESTUDIANTE, id);
	}
}
