package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.PeriodoDto;
import co.edu.unbosque.arbolado.dto.PeriodoRequest;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.ConflictoException;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.exception.ReglaNegocioException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.repository.PeriodoRepository;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Periodos académicos: creación, modificación sin eliminación y activación. */
@Service
public class PeriodoService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "PERIODO";

	/** Repositorio de periodos. */
	private final PeriodoRepository periodos;

	/** Consulta de inscripciones sin nota definitiva. */
	private final PendientesDePeriodo pendientes;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Crea el servicio.
	 *
	 * @param periodos   repositorio de periodos
	 * @param pendientes consulta de inscripciones sin nota definitiva
	 * @param auditoria  registro de auditoría
	 * @param mapper     conversor de entidades a DTO
	 */
	public PeriodoService(PeriodoRepository periodos, PendientesDePeriodo pendientes, AuditoriaService auditoria,
						  ModelMapper mapper) {
		this.periodos = periodos;
		this.pendientes = pendientes;
		this.auditoria = auditoria;
		this.mapper = mapper;
	}

	/**
	 * Crea un periodo inactivo.
	 *
	 * @param solicitud datos del periodo
	 * @param admin     administrador que lo crea
	 * @return el periodo creado
	 * @throws ConflictoException si el código ya existe
	 */
	@Transactional
	public PeriodoDto crear(PeriodoRequest solicitud, UsuarioAutenticado admin) {
		if (periodos.existsByCodigo(solicitud.getCodigo())) {
			throw rechazarCodigo(AccionAuditoria.CREACION_PERIODO, admin, null, solicitud.getCodigo());
		}
		Periodo periodo;
		try {
			periodo = periodos.saveAndFlush(
					new Periodo(solicitud.getCodigo(), solicitud.getFechaInicio(), solicitud.getFechaFin()));
		} catch (DataIntegrityViolationException e) {
			// Otra petición creó el mismo código entre la consulta y el guardado.
			throw rechazarCodigo(AccionAuditoria.CREACION_PERIODO, admin, null, solicitud.getCodigo());
		}
		auditoria.registrarExitoso(AccionAuditoria.CREACION_PERIODO, admin.id(), admin.correo(), ENTIDAD,
				String.valueOf(periodo.getId()), "Periodo " + periodo.getCodigo());
		return mapper.map(periodo, PeriodoDto.class);
	}

	/**
	 * Modifica código y fechas de un periodo; no cambia si está activo ni permite eliminarlo.
	 *
	 * @param id        periodo a modificar
	 * @param solicitud datos nuevos
	 * @param admin     administrador que lo modifica
	 * @return el periodo modificado
	 * @throws NoEncontradoException si no existe el periodo
	 * @throws ConflictoException    si otro periodo ya usa el código
	 */
	@Transactional
	public PeriodoDto modificar(Long id, PeriodoRequest solicitud, UsuarioAutenticado admin) {
		Periodo periodo = buscar(id);
		if (periodos.existsByCodigoAndIdNot(solicitud.getCodigo(), id)) {
			throw rechazarCodigo(AccionAuditoria.MODIFICACION_PERIODO, admin, id, solicitud.getCodigo());
		}
		String anterior = periodo.getCodigo() + " " + periodo.getFechaInicio() + "/" + periodo.getFechaFin();
		periodo.actualizar(solicitud.getCodigo(), solicitud.getFechaInicio(), solicitud.getFechaFin());
		try {
			periodos.saveAndFlush(periodo);
		} catch (DataIntegrityViolationException e) {
			throw rechazarCodigo(AccionAuditoria.MODIFICACION_PERIODO, admin, id, solicitud.getCodigo());
		}
		auditoria.registrarExitoso(AccionAuditoria.MODIFICACION_PERIODO, admin.id(), admin.correo(), ENTIDAD,
				String.valueOf(id), "Antes: " + anterior + "; ahora: " + periodo.getCodigo() + " "
						+ periodo.getFechaInicio() + "/" + periodo.getFechaFin());
		return mapper.map(periodo, PeriodoDto.class);
	}

	/**
	 * Activa un periodo y desactiva el vigente en la misma transacción. Si el vigente tiene inscripciones
	 * sin nota definitiva se rechaza: quedarían sin poder calificarse. Activar el que ya
	 * está activo no cambia nada.
	 *
	 * @param id    periodo a activar
	 * @param admin administrador que lo activa
	 * @return el periodo activado
	 * @throws NoEncontradoException si no existe el periodo
	 * @throws ReglaNegocioException si el periodo vigente tiene notas pendientes, o si otra activación simultánea ganó
	 */
	@Transactional
	public PeriodoDto activar(Long id, UsuarioAutenticado admin) {
		Periodo objetivo = buscar(id);
		if (objetivo.isActivo()) {
			return mapper.map(objetivo, PeriodoDto.class);
		}

		Optional<Periodo> vigente = periodos.findFirstByActivoTrue();
		if (vigente.isPresent()) {
			long sinNota = pendientes.contarSinDefinitiva(vigente.get().getId());
			if (sinNota > 0) {
				String mensaje = "El periodo " + vigente.get().getCodigo() + " tiene " + sinNota
						+ " inscripciones sin nota definitiva; cárgalas antes de activar otro periodo";
				auditoria.registrarFallido(AccionAuditoria.MODIFICACION_PERIODO, admin.id(), admin.correo(), ENTIDAD,
						String.valueOf(id), "Activación rechazada: " + sinNota + " inscripciones sin definitiva en "
								+ vigente.get().getCodigo());
				throw new ReglaNegocioException(CodigoError.PERIODO_CON_NOTAS_PENDIENTES, mensaje);
			}
			// Primero se libera la marca: el índice único no admite dos periodos activos ni por un instante.
			vigente.get().desactivar();
			periodos.saveAndFlush(vigente.get());
		}

		objetivo.activar();
		try {
			periodos.saveAndFlush(objetivo);
		} catch (DataIntegrityViolationException e) {
			throw new ReglaNegocioException(CodigoError.REGLA_NEGOCIO,
					"Otro administrador activó un periodo al mismo tiempo; vuelve a intentarlo");
		}
		auditoria.registrarExitoso(AccionAuditoria.MODIFICACION_PERIODO, admin.id(), admin.correo(), ENTIDAD,
				String.valueOf(id), "Periodo " + objetivo.getCodigo() + " activado"
						+ vigente.map(v -> "; se desactivó " + v.getCodigo()).orElse(""));
		return mapper.map(objetivo, PeriodoDto.class);
	}

	/**
	 * Lista todos los periodos, del más reciente al más antiguo.
	 *
	 * @return los periodos
	 */
	@Transactional(readOnly = true)
	public List<PeriodoDto> listar() {
		return periodos.findAllByOrderByCodigoDesc().stream().map(p -> mapper.map(p, PeriodoDto.class)).toList();
	}

	/**
	 * Consulta el periodo activo.
	 *
	 * @return el periodo activo
	 * @throws NoEncontradoException si no hay periodo activo
	 */
	@Transactional(readOnly = true)
	public PeriodoDto activo() {
		return periodos.findByActivoTrue().map(p -> mapper.map(p, PeriodoDto.class))
				.orElseThrow(() -> new NoEncontradoException("No hay un periodo activo"));
	}

	/**
	 * Busca un periodo o lanza el 404.
	 *
	 * @param id identificador del periodo
	 * @return el periodo
	 */
	private Periodo buscar(Long id) {
		return periodos.findById(id).orElseThrow(() -> new NoEncontradoException("No existe un periodo con ese identificador"));
	}

	/**
	 * Audita un código repetido y arma el conflicto.
	 *
	 * @param accion    acción rechazada
	 * @param admin     administrador que lo intentó
	 * @param idRecurso periodo afectado o {@code null} si se estaba creando
	 * @param codigo    código repetido
	 * @return la excepción lista para lanzar
	 */
	private ConflictoException rechazarCodigo(AccionAuditoria accion, UsuarioAutenticado admin, Long idRecurso,
											  String codigo) {
		String mensaje = "Ya existe un periodo con el código " + codigo;
		auditoria.registrarFallido(accion, admin.id(), admin.correo(), ENTIDAD,
				idRecurso == null ? null : String.valueOf(idRecurso), mensaje);
		return new ConflictoException(CodigoError.PERIODO_DUPLICADO, mensaje);
	}
}
