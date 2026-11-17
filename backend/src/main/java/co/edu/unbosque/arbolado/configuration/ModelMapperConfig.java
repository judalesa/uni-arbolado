package co.edu.unbosque.arbolado.configuration;

import co.edu.unbosque.arbolado.dto.AsignaturaDto;
import co.edu.unbosque.arbolado.dto.AsignaturaResumenDto;
import co.edu.unbosque.arbolado.model.Asignatura;
import co.edu.unbosque.arbolado.model.Prerrequisito;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

/** Configuración de ModelMapper, que convierte entidades en DTO dentro de los servicios. */
@Configuration
public class ModelMapperConfig {

	/**
	 * Crea el conversor con coincidencia estricta de nombres y la regla de prerrequisitos.
	 *
	 * @return instancia compartida de ModelMapper
	 */
	@Bean
	public ModelMapper modelMapper() {
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		// La entidad guarda relaciones Prerrequisito; el DTO expone directamente la asignatura requisito.
		Converter<List<Prerrequisito>, List<AsignaturaResumenDto>> aRequisitos = contexto ->
				contexto.getSource().stream()
						.map(Prerrequisito::getRequisito)
						.sorted(Comparator.comparing(Asignatura::getCodigo))
						.map(requisito -> mapper.map(requisito, AsignaturaResumenDto.class))
						.toList();

		mapper.typeMap(Asignatura.class, AsignaturaDto.class).addMappings(mapeo ->
				mapeo.using(aRequisitos).map(Asignatura::getPrerrequisitos, AsignaturaDto::setPrerrequisitos));
		return mapper;
	}
}
