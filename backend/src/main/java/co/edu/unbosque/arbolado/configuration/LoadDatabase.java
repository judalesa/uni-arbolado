package co.edu.unbosque.arbolado.configuration;

import co.edu.unbosque.arbolado.service.AdministradorInicialService;
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
	 * Define el ejecutor de arranque que precarga el plan de estudios y el administrador.
	 *
	 * @param carga         servicio de precarga idempotente del plan de estudios
	 * @param administrador servicio de precarga idempotente del administrador
	 * @return tarea que Spring ejecuta al terminar de iniciar
	 */
	@Bean
	public CommandLineRunner cargarDatosIniciales(CargaInicialService carga,
												  AdministradorInicialService administrador) {
		return args -> {
			LOG.info("Administrador precargado: {}", administrador.crearSiNoExiste() ? "creado" : "ya existía");
			CargaInicialService.ResultadoCarga resultado = carga.cargarPlanDeEstudios();
			LOG.info("Carga inicial: {} asignaturas y {} prerrequisitos nuevos; en la base hay {} asignaturas y {} prerrequisitos",
					resultado.asignaturasNuevas(), resultado.prerrequisitosNuevos(),
					resultado.totalAsignaturas(), resultado.totalPrerrequisitos());
		};
	}
}
