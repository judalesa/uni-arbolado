package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Precarga el administrador con las credenciales de la configuración local. */
@Service
public class AdministradorInicialService {

	/** Repositorio de usuarios. */
	private final UsuarioRepository usuarios;

	/** Codificador de contraseñas. */
	private final CodificadorContrasena codificador;

	/** Configuración de la aplicación. */
	private final PropiedadesApp propiedades;

	/**
	 * Crea el servicio.
	 *
	 * @param usuarios    repositorio de usuarios
	 * @param codificador codificador de contraseñas
	 * @param propiedades configuración de la aplicación
	 */
	public AdministradorInicialService(UsuarioRepository usuarios, CodificadorContrasena codificador,
									   PropiedadesApp propiedades) {
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.propiedades = propiedades;
	}

	/**
	 * Crea el administrador si todavía no existe uno. Es idempotente: si ya hay una cuenta con ese correo
	 * y rol administrador no la toca, y por eso cambiar la clave en la configuración no cambia la de la
	 * cuenta existente.
	 *
	 * @return {@code true} si se creó la cuenta en esta ejecución
	 * @throws IllegalStateException si el correo o el documento ya pertenecen a otra cuenta que no es del
	 *                               administrador, porque la configuración es inconsistente
	 */
	@Transactional
	public boolean crearSiNoExiste() {
		PropiedadesApp.Admin admin = propiedades.admin();
		String correo = admin.correo().trim().toLowerCase(Locale.ROOT);

		var existente = usuarios.findByCorreo(correo);
		if (existente.isPresent()) {
			if (existente.get().getRol() != Rol.ADMINISTRADOR) {
				throw new IllegalStateException("El correo de app.admin.correo ya pertenece a una cuenta que no es de administrador");
			}
			return false;
		}
		if (usuarios.existsByDocumento(admin.documento())) {
			throw new IllegalStateException("El documento de app.admin.documento ya pertenece a otra cuenta");
		}
		usuarios.save(new Usuario(admin.documento(), admin.nombre(), correo,
				codificador.encode(admin.contrasena()), Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO));
		return true;
	}
}
