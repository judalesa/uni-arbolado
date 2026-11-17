package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
	 * Busca una cuenta por documento.
	 *
	 * @param documento documento de 10 dígitos
	 * @return la cuenta, si existe
	 */
	Optional<Usuario> findByDocumento(String documento);

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

	/**
	 * Busca cuentas de un rol cuyo nombre, documento o correo contenga el texto. El texto llega ya en
	 * minúsculas y con los comodines escapados con {@code !}; vacío devuelve todas.
	 *
	 * @param rol      rol de las cuentas
	 * @param texto    fragmento a buscar, en minúsculas y escapado
	 * @param paginado página y orden pedidos
	 * @return la página de cuentas
	 */
	@Query("""
			select u from Usuario u
			where u.rol = :rol
			  and (lower(u.nombreCompleto) like concat('%', :texto, '%') escape '!'
			    or u.documento like concat('%', :texto, '%') escape '!'
			    or lower(u.correo) like concat('%', :texto, '%') escape '!')
			""")
	Page<Usuario> buscar(@Param("rol") Rol rol, @Param("texto") String texto, Pageable paginado);
}
