package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.AsignaturaDto;
import co.edu.unbosque.arbolado.service.AsignaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta del plan de estudios (RF-17). Solo lectura, para estudiantes y administradores. */
@RestController
@RequestMapping("/api/asignaturas")
@Tag(name = "Plan de estudios")
@PreAuthorize("hasAnyRole('ESTUDIANTE', 'ADMINISTRADOR')")
public class AsignaturaController {

	/** Servicio de consulta del plan. */
	private final AsignaturaService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de consulta del plan
	 */
	public AsignaturaController(AsignaturaService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Lista las 34 asignaturas con sus prerrequisitos.
	 *
	 * @return el plan de estudios completo
	 */
	@GetMapping
	@Operation(summary = "Consultar el plan de estudios")
	public List<AsignaturaDto> listar() {
		return servicio.listar();
	}

	/**
	 * Consulta una asignatura.
	 *
	 * @param id identificador de la asignatura
	 * @return la asignatura con sus prerrequisitos
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Consultar una asignatura")
	public AsignaturaDto obtener(@PathVariable Long id) {
		return servicio.obtener(id);
	}
}
