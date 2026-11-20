package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.dto.InscripcionRequest;
import co.edu.unbosque.arbolado.dto.Motivo;
import co.edu.unbosque.arbolado.exception.AccesoDenegadoException;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.CodigoMotivo;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.exception.ReglaNegocioException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.EstadoInscripcion;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Horario;
import co.edu.unbosque.arbolado.model.Inscripcion;
import co.edu.unbosque.arbolado.model.Prerrequisito;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.GrupoRepository;
import co.edu.unbosque.arbolado.repository.InscripcionRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inscripción de un estudiante en un grupo. Las validaciones se
 * evalúan todas y se informan juntas; si alguna falla no se inscribe nada.
 */
@Service
public class InscripcionService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "INSCRIPCION";

	/** Máximo de créditos cursando a la vez. */
	static final int MAXIMO_CREDITOS = 12;

	/** Repositorio de inscripciones. */
	private final InscripcionRepository inscripciones;

	/** Repositorio de grupos. */
	private final GrupoRepository grupos;

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/** Conversor de inscripciones a DTO. */
	private final InscripcionConversor conversor;

	/**
	 * Crea el servicio.
	 *
	 * @param inscripciones repositorio de inscripciones
	 * @param grupos        repositorio de grupos
	 * @param usuarios      repositorio de usuarios
	 * @param auditoria     registro de auditoría
	 * @param conversor     conversor de inscripciones a DTO
	 */
	public InscripcionService(InscripcionRepository inscripciones, GrupoRepository grupos, UsuarioRepository usuarios,
							  AuditoriaService auditoria, InscripcionConversor conversor) {
		this.inscripciones = inscripciones;
		this.grupos = grupos;
		this.usuarios = usuarios;
		this.auditoria = auditoria;
		this.conversor = conversor;
	}

	/**
	 * Inscribe al estudiante autenticado en un grupo. Dentro de una sola transacción bloquea primero la fila
	 * del estudiante y luego la del grupo, siempre en ese orden para evitar interbloqueos (diseño 4.1), y
	 * evalúa todas las reglas. Un rechazo se audita en una transacción aparte para que no se pierda.
	 *
	 * @param solicitud grupo elegido
	 * @param actor     estudiante autenticado; sale del token, nunca de la petición
	 * @return la inscripción creada, cursando
	 * @throws NoEncontradoException   si el estudiante o el grupo no existen
	 * @throws AccesoDenegadoException si la cuenta no está activa
	 * @throws ReglaNegocioException   con todos los motivos si alguna regla no se cumple
	 */
	@Transactional
	public InscripcionDto inscribir(InscripcionRequest solicitud, UsuarioAutenticado actor) {
		Usuario estudiante = usuarios.findConBloqueoById(actor.id())
				.orElseThrow(() -> NoEncontradoException.de("El estudiante", actor.id()));
		if (estudiante.getEstado() != EstadoCuenta.ACTIVO) {
			throw new AccesoDenegadoException(CodigoError.CUENTA_PENDIENTE, "Verifica tu correo antes de inscribir");
		}
		Grupo grupo = grupos.findConBloqueoById(solicitud.getGrupoId())
				.orElseThrow(() -> NoEncontradoException.de("El grupo", solicitud.getGrupoId()));

		List<Motivo> motivos = evaluar(estudiante, grupo);
		if (!motivos.isEmpty()) {
			throw rechazar(actor, grupo, motivos);
		}
		Inscripcion nueva;
		try {
			nueva = inscripciones.saveAndFlush(new Inscripcion(estudiante, grupo));
		} catch (DataIntegrityViolationException e) {
			// Red de seguridad: las restricciones únicas atrapan lo que el bloqueo no hubiera evitado.
			throw rechazar(actor, grupo, List.of(Motivo.de(CodigoMotivo.ASIGNATURA_YA_CURSANDO,
					"Ya tienes vigente " + nombre(grupo.getAsignatura()))));
		}
		auditoria.registrarExitoso(AccionAuditoria.INSCRIPCION, actor.id(), actor.correo(), ENTIDAD,
				String.valueOf(nueva.getId()), "Inscrito en " + etiqueta(grupo));
		return conversor.aDto(nueva);
	}

	/**
	 * Evalúa todas las reglas de inscripción sin detenerse en la primera que falla.
	 *
	 * @param estudiante estudiante ya bloqueado
	 * @param grupo      grupo ya bloqueado
	 * @return los motivos de rechazo; vacío si puede inscribirse
	 */
	private List<Motivo> evaluar(Usuario estudiante, Grupo grupo) {
		List<Motivo> motivos = new ArrayList<>();
		Asignatura asignatura = grupo.getAsignatura();
		Set<Long> aprobadas = inscripciones.asignaturasAprobadas(estudiante.getId());
		List<Inscripcion> cursando = inscripciones.findByEstudianteIdAndEstado(estudiante.getId(),
				EstadoInscripcion.CURSANDO);

		validarPeriodo(grupo, motivos);
		validarCupo(grupo, motivos);
		validarDuplicada(asignatura, aprobadas, cursando, motivos);
		validarPrerrequisitos(asignatura, aprobadas, motivos);
		validarCreditos(asignatura, cursando, motivos);
		validarHorarios(grupo, cursando, motivos);
		return motivos;
	}

	/**
	 * Solo se inscribe en grupos del periodo activo.
	 *
	 * @param grupo   grupo elegido
	 * @param motivos lista a la que se agrega el motivo
	 */
	private static void validarPeriodo(Grupo grupo, List<Motivo> motivos) {
		if (!grupo.getPeriodo().isActivo()) {
			motivos.add(Motivo.de(CodigoMotivo.PERIODO_NO_ACTIVO, "El periodo " + grupo.getPeriodo().getCodigo()
					+ " no está activo; solo puedes inscribir grupos del periodo activo"));
		}
	}

	/**
	 * El grupo no puede tener ya 30 estudiantes cursando.
	 *
	 * @param grupo   grupo elegido
	 * @param motivos lista a la que se agrega el motivo
	 */
	private void validarCupo(Grupo grupo, List<Motivo> motivos) {
		long cursando = inscripciones.countByGrupoIdAndEstado(grupo.getId(), EstadoInscripcion.CURSANDO);
		if (cursando >= grupo.getCupoMaximo()) {
			motivos.add(Motivo.de(CodigoMotivo.GRUPO_LLENO, etiqueta(grupo) + " está lleno ("
					+ cursando + " de " + grupo.getCupoMaximo() + " cupos)"));
		}
	}

	/**
	 * La asignatura no puede estar ya aprobada ni cursándose en otro grupo.
	 *
	 * @param asignatura asignatura del grupo elegido
	 * @param aprobadas  asignaturas aprobadas del estudiante
	 * @param cursando   inscripciones cursando del estudiante
	 * @param motivos    lista a la que se agregan los motivos
	 */
	private static void validarDuplicada(Asignatura asignatura, Set<Long> aprobadas, List<Inscripcion> cursando,
										 List<Motivo> motivos) {
		if (aprobadas.contains(asignatura.getId())) {
			motivos.add(Motivo.de(CodigoMotivo.ASIGNATURA_YA_APROBADA, "Ya aprobaste " + nombre(asignatura)));
		}
		cursando.stream().filter(i -> i.getAsignatura().getId().equals(asignatura.getId())).findFirst()
				.ifPresent(i -> motivos.add(Motivo.de(CodigoMotivo.ASIGNATURA_YA_CURSANDO,
						"Ya estás cursando " + nombre(asignatura) + " en el grupo " + i.getGrupo().getNumero())));
	}

	/**
	 * Cada prerrequisito debe estar aprobado; cursarlo no basta. Se informa uno por cada faltante.
	 *
	 * @param asignatura asignatura del grupo elegido
	 * @param aprobadas  asignaturas aprobadas del estudiante
	 * @param motivos    lista a la que se agregan los motivos
	 */
	private static void validarPrerrequisitos(Asignatura asignatura, Set<Long> aprobadas, List<Motivo> motivos) {
		for (Prerrequisito p : asignatura.getPrerrequisitos()) {
			if (!aprobadas.contains(p.getRequisito().getId())) {
				motivos.add(Motivo.de(CodigoMotivo.PREREQUISITO_NO_CUMPLIDO,
						"Falta aprobar " + nombre(p.getRequisito()) + ", prerrequisito de " + nombre(asignatura)));
			}
		}
	}

	/**
	 * Los créditos cursando más los de esta asignatura no pueden pasar de 12.
	 *
	 * @param asignatura asignatura del grupo elegido
	 * @param cursando   inscripciones cursando del estudiante
	 * @param motivos    lista a la que se agrega el motivo
	 */
	private static void validarCreditos(Asignatura asignatura, List<Inscripcion> cursando, List<Motivo> motivos) {
		int actuales = cursando.stream().mapToInt(i -> i.getAsignatura().getCreditos()).sum();
		if (actuales + asignatura.getCreditos() > MAXIMO_CREDITOS) {
			motivos.add(Motivo.de(CodigoMotivo.CREDITOS_EXCEDIDOS, "Superarías el máximo de " + MAXIMO_CREDITOS
					+ " créditos por periodo (cursas " + actuales + " y esta asignatura suma " + asignatura.getCreditos() + ")"));
		}
	}

	/**
	 * El horario no puede traslaparse con el de otro grupo cursando; empezar justo cuando otro termina no
	 * es traslape. Se informa una vez por cada asignatura con la que choca.
	 *
	 * @param grupo    grupo elegido
	 * @param cursando inscripciones cursando del estudiante
	 * @param motivos  lista a la que se agregan los motivos
	 */
	private static void validarHorarios(Grupo grupo, List<Inscripcion> cursando, List<Motivo> motivos) {
		for (Inscripcion otra : cursando) {
			boolean choca = grupo.getHorarios().stream().anyMatch(nuevo -> otra.getGrupo().getHorarios().stream()
					.anyMatch(existente -> seTraslapan(nuevo, existente)));
			if (choca) {
				motivos.add(Motivo.de(CodigoMotivo.HORARIO_TRASLAPADO, "El horario se traslapa con "
						+ nombre(otra.getAsignatura()) + " (grupo " + otra.getGrupo().getNumero() + ")"));
			}
		}
	}

	/**
	 * Indica si dos franjas se pisan el mismo día.
	 *
	 * @param a una franja
	 * @param b la otra franja
	 * @return {@code true} si comparten algún minuto
	 */
	private static boolean seTraslapan(Horario a, Horario b) {
		return a.getDiaSemana() == b.getDiaSemana() && a.getHoraInicio().isBefore(b.getHoraFin())
				&& b.getHoraInicio().isBefore(a.getHoraFin());
	}

	/**
	 * Audita el rechazo en una transacción aparte y arma la excepción con todos los motivos.
	 *
	 * @param actor   estudiante que intentó inscribirse
	 * @param grupo   grupo elegido
	 * @param motivos todos los incumplimientos
	 * @return la excepción lista para lanzar
	 */
	private ReglaNegocioException rechazar(UsuarioAutenticado actor, Grupo grupo, List<Motivo> motivos) {
		String codigos = motivos.stream().map(Motivo::codigo).collect(Collectors.joining(", "));
		auditoria.registrarFallido(AccionAuditoria.INSCRIPCION, actor.id(), actor.correo(), ENTIDAD, null,
				"Rechazada en " + etiqueta(grupo) + ": " + codigos);
		return new ReglaNegocioException("No se pudo realizar la inscripción", motivos);
	}

	/**
	 * Nombre legible de una asignatura para los mensajes.
	 *
	 * @param asignatura asignatura
	 * @return código y nombre
	 */
	private static String nombre(Asignatura asignatura) {
		return asignatura.getNombre() + " (" + asignatura.getCodigo() + ")";
	}

	/**
	 * Etiqueta corta de un grupo para mensajes y auditoría.
	 *
	 * @param grupo grupo
	 * @return por ejemplo {@code grupo 1 de DSI101 del periodo 2026-2}
	 */
	private static String etiqueta(Grupo grupo) {
		return "el grupo " + grupo.getNumero() + " de " + grupo.getAsignatura().getCodigo() + " del periodo "
				+ grupo.getPeriodo().getCodigo();
	}
}
