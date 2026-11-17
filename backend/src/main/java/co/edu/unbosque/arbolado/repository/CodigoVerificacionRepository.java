package co.edu.unbosque.arbolado.repository;

import co.edu.unbosque.arbolado.model.CodigoVerificacion;
import co.edu.unbosque.arbolado.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link CodigoVerificacion}. */
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

	/**
	 * Códigos de la cuenta que todavía no se usaron ni se reemplazaron.
	 *
	 * @param usuario cuenta destinataria
	 * @return los códigos pendientes (normalmente uno)
	 */
	List<CodigoVerificacion> findByUsuarioAndUsadoFalseAndInvalidadoFalse(Usuario usuario);

	/**
	 * Último código sin usar ni reemplazar, con bloqueo de escritura: dos verificaciones simultáneas se
	 * turnan y no pueden superar los 5 intentos ni usar el código dos veces.
	 *
	 * @param usuario cuenta destinataria
	 * @return el código más reciente, si existe
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<CodigoVerificacion> findFirstByUsuarioAndUsadoFalseAndInvalidadoFalseOrderByIdDesc(Usuario usuario);

	/**
	 * Último código enviado a la cuenta, sin importar su estado; sirve para la espera entre reenvíos.
	 *
	 * @param usuario cuenta destinataria
	 * @return el código más reciente, si existe
	 */
	Optional<CodigoVerificacion> findFirstByUsuarioOrderByIdDesc(Usuario usuario);
}
