package co.edu.unbosque.arbolado.security;

import co.edu.unbosque.arbolado.model.Rol;

import java.time.Instant;

/**
 * Identidad que sale del JWT y queda en el contexto de seguridad. Los servicios toman de aquí el usuario
 * que actúa, así ningún endpoint recibe un identificador de usuario por parámetro.
 *
 * @param id        identificador del usuario
 * @param correo    correo del usuario
 * @param rol       rol de la cuenta
 * @param jti       identificador del token, usado para revocarlo en el logout
 * @param expiraEn  instante en que el token vence
 */
public record UsuarioAutenticado(Long id, String correo, Rol rol, String jti, Instant expiraEn) {
}
