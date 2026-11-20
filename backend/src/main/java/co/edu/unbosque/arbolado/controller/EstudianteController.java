package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.AsignaturaEstadoDto;
import co.edu.unbosque.arbolado.dto.ClaseDto;
import co.edu.unbosque.arbolado.dto.HistorialPeriodoDto;
import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.ConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultas del estudiante sobre sí mismo: inscripciones, plan, horario e historial. */
@RestController
@RequestMapping("/api/estudiante")
@PreAuthorize("hasRole('ESTUDIANTE')")
@Tag(name = "Estudiante")
public class EstudianteController {

	/** Servicio de consultas. */
	private final ConsultaService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de consultas
	 */
	public EstudianteController(ConsultaService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Mis inscripciones y notas.
	 *
	 * @param periodoId  periodo a consultar; sin él, el activo
	 * @param estudiante estudiante autenticado
	 * @return las inscripciones
	 */
	@GetMapping("/inscripciones")
	@Operation(summary = "Mis inscripciones y notas")
	public List<InscripcionDto> inscripciones(@RequestParam(required = false) Long periodoId,
											  @AuthenticationPrincipal UsuarioAutenticado estudiante) {
		return servicio.inscripcionesDelEstudiante(estudiante.id(), periodoId);
	}

	/**
	 * Estado de cada asignatura del plan.
	 *
	 * @param estudiante estudiante autenticado
	 * @return el plan con estados
	 */
	@GetMapping("/plan-estado")
	@Operation(summary = "Estado de cada asignatura del plan")
	public List<AsignaturaEstadoDto> planEstado(@AuthenticationPrincipal UsuarioAutenticado estudiante) {
		return servicio.planConEstado(estudiante.id());
	}

	/**
	 * Mi horario del periodo activo.
	 *
	 * @param estudiante estudiante autenticado
	 * @return las franjas
	 */
	@GetMapping("/horario")
	@Operation(summary = "Mi horario del periodo activo")
	public List<ClaseDto> horario(@AuthenticationPrincipal UsuarioAutenticado estudiante) {
		return servicio.horarioDelEstudiante(estudiante.id());
	}

	/**
	 * Mi historial académico.
	 *
	 * @param estudiante estudiante autenticado
	 * @return los intentos por periodo
	 */
	@GetMapping("/historial")
	@Operation(summary = "Mi historial académico")
	public List<HistorialPeriodoDto> historial(@AuthenticationPrincipal UsuarioAutenticado estudiante) {
		return servicio.historialDelEstudiante(estudiante.id());
	}
}
