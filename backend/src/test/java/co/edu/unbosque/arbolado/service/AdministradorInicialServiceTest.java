package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de la precarga del administrador. */
@SpringBootTest
@ActiveProfiles("test")
class AdministradorInicialServiceTest {

	/** Servicio bajo prueba. */
	@Autowired
	private AdministradorInicialService servicio;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Configuración de la aplicación. */
	@Autowired
	private PropiedadesApp propiedades;

	/** Al arrancar ya se creó el administrador, activo y con la clave cifrada. */
	@Test
	void elAdministradorQuedaCreadoAlArrancar() {
		Usuario admin = usuarios.findByCorreo(propiedades.admin().correo()).orElseThrow();

		assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR);
		assertThat(admin.getEstado()).isEqualTo(EstadoCuenta.ACTIVO);
		assertThat(admin.getContrasena()).doesNotContain(propiedades.admin().contrasena());
		assertThat(codificador.matches(propiedades.admin().contrasena(), admin.getContrasena())).isTrue();
	}

	/** Repetir la precarga no crea otro administrador ni cambia el existente. */
	@Test
	void repetirLaPrecargaNoDuplica() {
		long antes = usuarios.count();
		String claveGuardada = usuarios.findByCorreo(propiedades.admin().correo()).orElseThrow().getContrasena();

		assertThat(servicio.crearSiNoExiste()).isFalse();

		assertThat(usuarios.count()).isEqualTo(antes);
		assertThat(usuarios.findByCorreo(propiedades.admin().correo()).orElseThrow().getContrasena())
				.isEqualTo(claveGuardada);
	}
}
