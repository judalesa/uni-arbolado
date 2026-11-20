package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.dto.AuditoriaDto;
import co.edu.unbosque.arbolado.dto.PaginaDto;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Resultado;
import co.edu.unbosque.arbolado.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/** Consulta de la auditoría; exclusiva del administrador y solo de lectura. */
@RestController
@RequestMapping("/api/admin/auditoria")
@Tag(name = "Auditoría")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaController {

	/** Servicio de auditoría. */
	private final AuditoriaService servicio;

	/**
	 * Crea el controlador.
	 *
	 * @param servicio servicio de auditoría
	 */
	public AuditoriaController(AuditoriaService servicio) {
		this.servicio = servicio;
	}

	/**
	 * Consulta la auditoría con filtros opcionales.
	 *
	 * @param usuarioId usuario que originó el evento
	 * @param accion    evento
	 * @param resultado exitoso o fallido
	 * @param desde     fecha y hora inicial con zona, inclusive
	 * @param hasta     fecha y hora final con zona, inclusive
	 * @param pagina    número de página, desde 0
	 * @param tamano    elementos por página
	 * @return la página de registros, del más reciente al más antiguo
	 */
	@GetMapping
	@Operation(summary = "Consultar la auditoría")
	public PaginaDto<AuditoriaDto> consultar(
			@RequestParam(required = false) Long usuarioId,
			@RequestParam(required = false) AccionAuditoria accion,
			@RequestParam(required = false) Resultado resultado,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamano) {
		return servicio.consultar(usuarioId, accion, resultado, desde == null ? null : desde.toInstant(),
				hasta == null ? null : hasta.toInstant(), pagina, tamano);
	}
}
