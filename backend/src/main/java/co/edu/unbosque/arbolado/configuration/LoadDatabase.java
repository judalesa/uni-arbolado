package co.edu.unbosque.arbolado.configuration;

import co.edu.unbosque.arbolado.service.CargaInicialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ejecuta la precarga de datos base cada vez que arranca la aplicación. */
@Configuration
public class LoadDatabase {

	/** Registro de la aplicación. */
	private static final Logger LOG = LoggerFactory.getLogger(LoadDatabase.class);

	/**
	 * Define el ejecutor de arranque que precarga el plan de estudios.
	 *
	 * @param carga servicio de precarga idempotente
	 * @return tarea que Spring ejecuta al terminar de iniciar
	 */
	@Bean
	public CommandLineRunner cargarDatosIniciales(CargaInicialService carga) {
		return args -> {
			CargaInicialService.ResultadoCarga resultado = carga.cargarPlanDeEstudios();
			LOG.info("Carga inicial: {} asignaturas y {} prerrequisitos nuevos",
					resultado.asignaturasNuevas(), resultado.prerrequisitosNuevos());
		};
	}
}
