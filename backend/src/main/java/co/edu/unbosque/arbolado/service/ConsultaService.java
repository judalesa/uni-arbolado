package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.AsignaturaDto;
import co.edu.unbosque.arbolado.dto.AsignaturaEstadoDto;
import co.edu.unbosque.arbolado.dto.AsignaturaResumenDto;
import co.edu.unbosque.arbolado.dto.ClaseDto;
import co.edu.unbosque.arbolado.dto.HistorialPeriodoDto;
import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.dto.PeriodoDto;
import co.edu.unbosque.arbolado.exception.AccesoDenegadoException;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.model.EstadoAsignatura;
import co.edu.unbosque.arbolado.model.EstadoInscripcion;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Horario;
import co.edu.unbosque.arbolado.model.Inscripcion;
import co.edu.unbosque.arbolado.model.Periodo;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.repository.GrupoRepository;
import co.edu.unbosque.arbolado.repository.InscripcionRepository;
import co.edu.unbosque.arbolado.repository.PeriodoRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de solo lectura del estudiante y del profesor (RF-25, RF-35 a RF-39, RF-44, RF-45). Quien consulta
 * siempre sale del token: nadie puede pedir los datos de otro cambiando un parámetro.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaService {

	/** Orden del horario: por día y luego por hora de inicio. */
	private static final Comparator<ClaseDto> ORDEN_HORARIO = Comparator.comparing(ClaseDto::diaSemana)
			.thenComparing(ClaseDto::horaInicio);

	/** Repositorio de inscripciones. */
	private final InscripcionRepository inscripciones;

	/** Repositorio de grupos. */
	private final GrupoRepository grupos;

	/** Repositorio de periodos. */
	private final PeriodoRepository periodos;

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Servicio del plan de estudios. */
	private final AsignaturaService servicioAsignaturas;

	/** Servicio de grupos. */
	private final GrupoService servicioGrupos;

	/** Conversor de inscripciones a DTO. */
	private final InscripcionConversor conversor;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Crea el servicio.
	 *
	 * @param inscripciones       repositorio de inscripciones
	 * @param grupos              repositorio de grupos
	 * @param periodos            repositorio de periodos
	 * @param usuarios            repositorio de usuarios
	 * @param servicioAsignaturas servicio del plan de estudios
	 * @param servicioGrupos      servicio de grupos
	 * @param conversor           conversor de inscripciones a DTO
	 * @param mapper              conversor de entidades a DTO
	 */
	public ConsultaService(InscripcionRepository inscripciones, GrupoRepository grupos, PeriodoRepository periodos,
						   UsuarioRepository usuarios, AsignaturaService servicioAsignaturas, GrupoService servicioGrupos,
						   InscripcionConversor conversor, ModelMapper mapper) {
		this.inscripciones = inscripciones;
		this.grupos = grupos;
		this.periodos = periodos;
		this.usuarios = usuarios;
		this.servicioAsignaturas = servicioAsignaturas;
		this.servicioGrupos = servicioGrupos;
		this.conversor = conversor;
		this.mapper = mapper;
	}

	/**
	 * Inscripciones del estudiante en un periodo, con sus notas (RF-35, RF-44).
	 *
	 * @param estudianteId estudiante autenticado
	 * @param periodoId    periodo a consultar; sin él, el activo
	 * @return las inscripciones; vacío si no se indica periodo y no hay uno activo
	 */
	public List<InscripcionDto> inscripcionesDelEstudiante(Long estudianteId, Long periodoId) {
		Long periodo = periodoId != null ? periodoId : periodos.findByActivoTrue().map(Periodo::getId).orElse(null);
		if (periodo == null) {
			return List.of();
		}
		return conversor.aDtos(inscripciones.findByEstudianteIdAndPeriodoIdOrderByAsignaturaCodigoAsc(estudianteId, periodo));
	}

	/**
	 * Estado de cada asignatura del plan para el estudiante (RF-35). Si hay varios intentos, manda el que
	 * aprobó; si no, el que cursa; si no, el último reprobado.
	 *
	 * @param estudianteId estudiante autenticado
	 * @return una fila por asignatura, en el orden del plan
	 */
	public List<AsignaturaEstadoDto> planConEstado(Long estudianteId) {
		Map<Long, Inscripcion> decisivo = new HashMap<>();
		for (Inscripcion intento : inscripciones.findByEstudianteIdOrderByPeriodoCodigoDescAsignaturaCodigoAsc(estudianteId)) {
			decisivo.merge(intento.getAsignatura().getId(), intento,
					(actual, otro) -> prioridad(otro) > prioridad(actual) ? otro : actual);
		}
		List<AsignaturaEstadoDto> plan = new ArrayList<>();
		for (AsignaturaDto asignatura : servicioAsignaturas.listar()) {
			Inscripcion intento = decisivo.get(asignatura.getId());
			plan.add(intento == null ? new AsignaturaEstadoDto(asignatura, EstadoAsignatura.POR_CURSAR, null)
					: new AsignaturaEstadoDto(asignatura, EstadoAsignatura.valueOf(intento.getEstado().name()),
							intento.getNotaDefinitiva()));
		}
		return plan;
	}

	/**
	 * Horario del estudiante en el periodo activo: una fila por franja de lo que está cursando (RF-36).
	 *
	 * @param estudianteId estudiante autenticado
	 * @return las franjas ordenadas por día y hora
	 */
	public List<ClaseDto> horarioDelEstudiante(Long estudianteId) {
		List<Grupo> cursados = inscripciones.findByEstudianteIdAndEstado(estudianteId, EstadoInscripcion.CURSANDO).stream()
				.map(Inscripcion::getGrupo)
				.filter(g -> g.getPeriodo().isActivo())
				.toList();
		return clases(cursados);
	}

	/**
	 * Historial académico del estudiante: todos los intentos agrupados por periodo, del más reciente al más
	 * antiguo (RF-38).
	 *
	 * @param estudianteId estudiante autenticado
	 * @return el historial
	 */
	public List<HistorialPeriodoDto> historialDelEstudiante(Long estudianteId) {
		List<Inscripcion> intentos = inscripciones.findByEstudianteIdOrderByPeriodoCodigoDescAsignaturaCodigoAsc(estudianteId);
		List<InscripcionDto> dtos = conversor.aDtos(intentos);
		Map<Long, List<InscripcionDto>> porPeriodo = new LinkedHashMap<>();
		Map<Long, PeriodoDto> periodosVistos = new HashMap<>();
		for (int i = 0; i < intentos.size(); i++) {
			Periodo periodo = intentos.get(i).getGrupo().getPeriodo();
			periodosVistos.computeIfAbsent(periodo.getId(), id -> mapper.map(periodo, PeriodoDto.class));
			porPeriodo.computeIfAbsent(periodo.getId(), id -> new ArrayList<>()).add(dtos.get(i));
		}
		return porPeriodo.entrySet().stream()
				.map(e -> new HistorialPeriodoDto(periodosVistos.get(e.getKey()), e.getValue()))
				.toList();
	}

	/**
	 * Estudiantes de un grupo del profesor, con sus notas (RF-39, RF-45).
	 *
	 * @param profesorId profesor autenticado
	 * @param grupoId    grupo a consultar
	 * @return las inscripciones del grupo en cualquier estado, por nombre del estudiante
	 * @throws NoEncontradoException   si el grupo no existe
	 * @throws AccesoDenegadoException si el grupo es de otro profesor
	 */
	public List<InscripcionDto> estudiantesDeGrupo(Long profesorId, Long grupoId) {
		Grupo grupo = grupos.findById(grupoId).orElseThrow(() -> NoEncontradoException.de("El grupo", grupoId));
		if (!grupo.getProfesor().getId().equals(profesorId)) {
			throw new AccesoDenegadoException(CodigoError.ACCESO_DENEGADO, "Ese grupo no está a tu cargo");
		}
		return conversor.aDtos(inscripciones.findByGrupoIdOrderByEstudianteNombreCompletoAsc(grupoId));
	}

	/**
	 * Horario del profesor en el periodo activo (RF-37).
	 *
	 * @param profesorId profesor autenticado
	 * @return las franjas de sus grupos ordenadas por día y hora
	 */
	public List<ClaseDto> horarioDelProfesor(Long profesorId) {
		return clases(servicioGrupos.delProfesorEnPeriodoActivo(profesorId));
	}

	/**
	 * Historial de un estudiante que tiene o tuvo alguna inscripción en un grupo del profesor (RF-45).
	 *
	 * @param profesorId   profesor autenticado
	 * @param estudianteId estudiante a consultar
	 * @return el historial completo del estudiante
	 * @throws NoEncontradoException   si no existe un estudiante con ese identificador
	 * @throws AccesoDenegadoException si el estudiante nunca ha estado en un grupo del profesor
	 */
	public List<HistorialPeriodoDto> historialParaProfesor(Long profesorId, Long estudianteId) {
		boolean esEstudiante = usuarios.findById(estudianteId).map(u -> u.getRol() == Rol.ESTUDIANTE).orElse(false);
		if (!esEstudiante) {
			throw NoEncontradoException.de("El estudiante", estudianteId);
		}
		if (!inscripciones.existsByEstudianteIdAndGrupoProfesorId(estudianteId, profesorId)) {
			throw new AccesoDenegadoException(CodigoError.ACCESO_DENEGADO,
					"Solo puedes consultar el historial de estudiantes de tus grupos");
		}
		return historialDelEstudiante(estudianteId);
	}

	/**
	 * Convierte los horarios de varios grupos en filas de horario.
	 *
	 * @param lista grupos con asignatura y horarios
	 * @return una fila por franja, ordenadas por día y hora
	 */
	private List<ClaseDto> clases(List<Grupo> lista) {
		List<ClaseDto> clases = new ArrayList<>();
		for (Grupo grupo : lista) {
			AsignaturaResumenDto asignatura = mapper.map(grupo.getAsignatura(), AsignaturaResumenDto.class);
			for (Horario h : grupo.getHorarios()) {
				clases.add(new ClaseDto(asignatura, grupo.getId(), grupo.getNumero(), grupo.getBloque(), grupo.getSalon(),
						h.getDiaSemana(), h.getHoraInicio().toString(), h.getHoraFin().toString()));
			}
		}
		clases.sort(ORDEN_HORARIO);
		return clases;
	}

	/**
	 * Peso de un intento para decidir el estado de la asignatura: aprobada pesa más que cursando, y cursando
	 * más que reprobada. Con el mismo peso se queda el primero, que es el más reciente.
	 *
	 * @param intento inscripción
	 * @return peso del intento
	 */
	private static int prioridad(Inscripcion intento) {
		return switch (intento.getEstado()) {
			case APROBADA -> 3;
			case CURSANDO -> 2;
			case REPROBADA -> 1;
		};
	}
}
