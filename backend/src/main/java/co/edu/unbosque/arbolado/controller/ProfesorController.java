package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.ClaseDto;
import co.edu.unbosque.arbolado.dto.HistorialPeriodoDto;
import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.dto.NotaRequest;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import co.edu.unbosque.arbolado.service.ConsultaService;
import co.edu.unbosque.arbolado.service.NotaService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consultas y registro de notas del profesor sobre sus grupos. La lista de sus grupos está en {@link GrupoController}. */
@RestController
@RequestMapping("/api/profesor")
@PreAuthorize("hasRole('PROFESOR')")
@Tag(name = "Profesor")
public class ProfesorController {

	/** Servicio de consultas. */
	private final ConsultaService servicio;

	/** Servicio de registro de notas. */
	private final NotaService notas;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de consultas
	 * @param notas    servicio de registro de notas
	 */
	public ProfesorController(ConsultaService servicio, NotaService notas) {
		this.servicio = servicio;
		this.notas = notas;
	}

	/**
	 * Estudiantes de uno de mis grupos, con sus notas.
	 *
	 * @param grupoId  grupo a consultar
	 * @param profesor profesor autenticado
	 * @return las inscripciones del grupo
	 */
	@GetMapping("/grupos/{grupoId}/estudiantes")
	@Operation(summary = "Estudiantes de uno de mis grupos")
	public List<InscripcionDto> estudiantes(@PathVariable Long grupoId,
											@AuthenticationPrincipal UsuarioAutenticado profesor) {
		return servicio.estudiantesDeGrupo(profesor.id(), grupoId);
	}

	/**
	 * Mi horario del periodo activo.
	 *
	 * @param profesor profesor autenticado
	 * @return las franjas
	 */
	@GetMapping("/horario")
	@Operation(summary = "Mi horario del periodo activo")
	public List<ClaseDto> horario(@AuthenticationPrincipal UsuarioAutenticado profesor) {
		return servicio.horarioDelProfesor(profesor.id());
	}

	/**
	 * Registra la nota de un corte de una inscripción de mis grupos.
	 *
	 * @param inscripcionId inscripción a calificar
	 * @param solicitud     corte y nota
	 * @param profesor      profesor autenticado
	 * @return la inscripción actualizada
	 */
	@PostMapping("/inscripciones/{inscripcionId}/notas")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrar la nota de un corte")
	public InscripcionDto registrarNota(@PathVariable Long inscripcionId, @Valid @RequestBody NotaRequest solicitud,
										@AuthenticationPrincipal UsuarioAutenticado profesor) {
		return notas.registrar(inscripcionId, solicitud, profesor);
	}

	/**
	 * Historial de un estudiante de mis grupos.
	 *
	 * @param estudianteId estudiante a consultar
	 * @param profesor     profesor autenticado
	 * @return los intentos por periodo
	 */
	@GetMapping("/estudiantes/{estudianteId}/historial")
	@Operation(summary = "Historial de un estudiante de mis grupos")
	public List<HistorialPeriodoDto> historial(@PathVariable Long estudianteId,
											   @AuthenticationPrincipal UsuarioAutenticado profesor) {
		return servicio.historialParaProfesor(profesor.id(), estudianteId);
	}
}
