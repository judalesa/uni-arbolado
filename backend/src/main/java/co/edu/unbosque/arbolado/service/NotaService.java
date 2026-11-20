package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.dto.NotaRequest;
import co.edu.unbosque.arbolado.exception.AccesoDenegadoException;
import co.edu.unbosque.arbolado.exception.ApiException;
import co.edu.unbosque.arbolado.exception.CodigoError;
import co.edu.unbosque.arbolado.exception.ConflictoException;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.exception.ReglaNegocioException;
import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Inscripcion;
import co.edu.unbosque.arbolado.repository.InscripcionRepository;
import co.edu.unbosque.arbolado.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de notas por corte. Cada corte se escribe una sola vez; con el tercero la
 * inscripción queda aprobada o reprobada y deja de ocupar cupo.
 */
@Service
public class NotaService {

	/** Tipo de recurso que se anota en la auditoría. */
	private static final String ENTIDAD = "INSCRIPCION";

	/** Repositorio de inscripciones. */
	private final InscripcionRepository inscripciones;

	/** Registro de auditoría. */
	private final AuditoriaService auditoria;

	/** Conversor de inscripciones a DTO. */
	private final InscripcionConversor conversor;

	/**
	 * Crea el servicio.
	 *
	 * @param inscripciones repositorio de inscripciones
	 * @param auditoria     registro de auditoría
	 * @param conversor     conversor de inscripciones a DTO
	 */
	public NotaService(InscripcionRepository inscripciones, AuditoriaService auditoria, InscripcionConversor conversor) {
		this.inscripciones = inscripciones;
		this.auditoria = auditoria;
		this.conversor = conversor;
	}

	/**
	 * Registra la nota de un corte. La inscripción se bloquea para que dos registros simultáneos del mismo
	 * corte no se pisen. Los rechazos se auditan en una transacción aparte.
	 *
	 * @param inscripcionId inscripción a calificar
	 * @param solicitud     corte y nota
	 * @param actor         profesor autenticado
	 * @return la inscripción actualizada
	 * @throws NoEncontradoException   si la inscripción no existe
	 * @throws AccesoDenegadoException si el grupo es de otro profesor
	 * @throws ReglaNegocioException   si el periodo del grupo no está activo
	 * @throws ConflictoException      si el corte ya tiene nota
	 */
	@Transactional
	public InscripcionDto registrar(Long inscripcionId, NotaRequest solicitud, UsuarioAutenticado actor) {
		Inscripcion inscripcion = inscripciones.findConBloqueoById(inscripcionId)
				.orElseThrow(() -> NoEncontradoException.de("La inscripción", inscripcionId));
		int corte = solicitud.getCorte();
		Grupo grupo = inscripcion.getGrupo();

		if (!grupo.getProfesor().getId().equals(actor.id())) {
			throw rechazar(actor, inscripcion, corte, new AccesoDenegadoException(CodigoError.ACCESO_DENEGADO,
					"Solo el profesor del grupo puede registrar sus notas"));
		}
		if (!grupo.getPeriodo().isActivo()) {
			throw rechazar(actor, inscripcion, corte, new ReglaNegocioException(CodigoError.PERIODO_NO_ACTIVO,
					"Solo se registran notas en el periodo activo; el periodo " + grupo.getPeriodo().getCodigo()
							+ " no lo está"));
		}
		if (inscripcion.notaDelCorte(corte) != null) {
			throw rechazar(actor, inscripcion, corte, new ConflictoException(CodigoError.NOTA_YA_REGISTRADA,
					"El corte " + corte + " ya tiene nota (" + inscripcion.notaDelCorte(corte)
							+ ") y las notas no se modifican"));
		}

		inscripcion.registrarNota(corte, solicitud.getValor());
		inscripciones.flush();
		auditoria.registrarExitoso(AccionAuditoria.REGISTRO_NOTA, actor.id(), actor.correo(), ENTIDAD,
				String.valueOf(inscripcion.getId()), detalle(inscripcion, corte));
		return conversor.aDto(inscripcion);
	}

	/**
	 * Audita un rechazo en una transacción aparte y devuelve la excepción para lanzarla.
	 *
	 * @param actor       profesor que lo intentó
	 * @param inscripcion inscripción afectada
	 * @param corte       corte intentado
	 * @param error       excepción a lanzar
	 * @return la misma excepción
	 */
	private ApiException rechazar(UsuarioAutenticado actor, Inscripcion inscripcion, int corte, ApiException error) {
		auditoria.registrarFallido(AccionAuditoria.REGISTRO_NOTA, actor.id(), actor.correo(), ENTIDAD,
				String.valueOf(inscripcion.getId()), "Corte " + corte + " rechazado: " + error.getCodigo());
		return error;
	}

	/**
	 * Texto de auditoría con el valor anterior y el nuevo y, si la hay, la definitiva.
	 *
	 * @param inscripcion inscripción ya calificada
	 * @param corte       corte registrado
	 * @return el detalle
	 */
	private static String detalle(Inscripcion inscripcion, int corte) {
		Grupo grupo = inscripcion.getGrupo();
		String texto = "Corte " + corte + ": anterior vacío, nuevo " + inscripcion.notaDelCorte(corte)
				+ ". Estudiante " + inscripcion.getEstudiante().getDocumento() + ", grupo " + grupo.getNumero()
				+ " de " + grupo.getAsignatura().getCodigo() + " del periodo " + grupo.getPeriodo().getCodigo();
		if (inscripcion.getNotaDefinitiva() != null) {
			texto += ". Definitiva " + inscripcion.getNotaDefinitiva() + ", " + inscripcion.getEstado();
		}
		return texto;
	}
}
