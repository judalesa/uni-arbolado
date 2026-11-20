package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.GrupoDto;
import co.edu.unbosque.arbolado.dto.InscripcionDto;
import co.edu.unbosque.arbolado.dto.UsuarioResumenDto;
import co.edu.unbosque.arbolado.model.Grupo;
import co.edu.unbosque.arbolado.model.Inscripcion;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Convierte inscripciones en DTO. Lo comparten la inscripción, las consultas y las notas. */
@Component
public class InscripcionConversor {

	/** Servicio de grupos, para el grupo con sus cupos. */
	private final GrupoService servicioGrupos;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Crea el conversor.
	 *
	 * @param servicioGrupos servicio de grupos
	 * @param mapper         conversor de entidades a DTO
	 */
	public InscripcionConversor(GrupoService servicioGrupos, ModelMapper mapper) {
		this.servicioGrupos = servicioGrupos;
		this.mapper = mapper;
	}

	/**
	 * Convierte una inscripción.
	 *
	 * @param inscripcion inscripción con estudiante y grupo accesibles
	 * @return el DTO
	 */
	public InscripcionDto aDto(Inscripcion inscripcion) {
		return aDto(inscripcion, servicioGrupos.detalle(inscripcion.getGrupo()));
	}

	/**
	 * Convierte varias inscripciones contando los cupos de todos sus grupos en una sola consulta.
	 *
	 * @param lista inscripciones con detalle
	 * @return los DTO en el mismo orden
	 */
	public List<InscripcionDto> aDtos(List<Inscripcion> lista) {
		Map<Long, GrupoDto> grupos = servicioGrupos.detalles(lista.stream().map(Inscripcion::getGrupo).toList());
		return lista.stream().map(i -> aDto(i, grupos.get(grupoId(i)))).toList();
	}

	/**
	 * Arma el DTO con el grupo ya convertido.
	 *
	 * @param inscripcion inscripción
	 * @param grupo       grupo con cupos
	 * @return el DTO
	 */
	private InscripcionDto aDto(Inscripcion inscripcion, GrupoDto grupo) {
		InscripcionDto dto = new InscripcionDto();
		dto.setId(inscripcion.getId());
		dto.setEstudiante(mapper.map(inscripcion.getEstudiante(), UsuarioResumenDto.class));
		dto.setGrupo(grupo);
		dto.setEstado(inscripcion.getEstado());
		dto.setNotaCorte1(inscripcion.getNotaCorte1());
		dto.setNotaCorte2(inscripcion.getNotaCorte2());
		dto.setNotaCorte3(inscripcion.getNotaCorte3());
		dto.setNotaDefinitiva(inscripcion.getNotaDefinitiva());
		dto.setFechaInscripcion(inscripcion.getFechaInscripcion());
		return dto;
	}

	/**
	 * Identificador del grupo de una inscripción.
	 *
	 * @param inscripcion inscripción
	 * @return identificador del grupo
	 */
	private static Long grupoId(Inscripcion inscripcion) {
		Grupo grupo = inscripcion.getGrupo();
		return grupo.getId();
	}
}
