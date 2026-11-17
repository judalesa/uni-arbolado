package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a datos de {@link Usuario}. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/**
	 * Busca una cuenta por correo.
	 *
	 * @param correo correo en minúsculas
	 * @return la cuenta, si existe
	 */
	Optional<Usuario> findByCorreo(String correo);

	/**
	 * Indica si ya existe una cuenta con ese correo.
	 *
	 * @param correo correo en minúsculas
	 * @return {@code true} si existe
	 */
	boolean existsByCorreo(String correo);

	/**
	 * Indica si ya existe una cuenta con ese documento.
	 *
	 * @param documento documento de 10 dígitos
	 * @return {@code true} si existe
	 */
	boolean existsByDocumento(String documento);
}
