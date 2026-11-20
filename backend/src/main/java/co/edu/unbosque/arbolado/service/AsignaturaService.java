package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.dto.AsignaturaDto;
import co.edu.unbosque.arbolado.exception.NoEncontradoException;
import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.repository.AsignaturaRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta del plan de estudios. Solo lectura: las asignaturas no se modifican. */
@Service
@Transactional(readOnly = true)
public class AsignaturaService {

	/** Repositorio de asignaturas. */
	private final AsignaturaRepository repositorio;

	/** Conversor de entidades a DTO. */
	private final ModelMapper mapper;

	/**
	 * Crea el servicio.
	 *
	 * @param repositorio repositorio de asignaturas
	 * @param mapper      conversor de entidades a DTO
	 */
	public AsignaturaService(AsignaturaRepository repositorio, ModelMapper mapper) {
		this.repositorio = repositorio;
		this.mapper = mapper;
	}

	/**
	 * Lista el plan completo ordenado por semestre y código.
	 *
	 * @return las asignaturas con sus prerrequisitos
	 */
	public List<AsignaturaDto> listar() {
		return repositorio.findAllByOrderBySemestreAscCodigoAsc().stream().map(this::aDto).toList();
	}

	/**
	 * Obtiene una asignatura.
	 *
	 * @param id identificador
	 * @return la asignatura con sus prerrequisitos
	 * @throws NoEncontradoException si no existe
	 */
	public AsignaturaDto obtener(Long id) {
		Asignatura asignatura = repositorio.findConPrerrequisitosById(id)
				.orElseThrow(() -> NoEncontradoException.de("La asignatura", id));
		return aDto(asignatura);
	}

	/**
	 * Convierte una entidad en su DTO.
	 *
	 * @param asignatura entidad
	 * @return DTO
	 */
	private AsignaturaDto aDto(Asignatura asignatura) {
		return mapper.map(asignatura, AsignaturaDto.class);
	}
}
