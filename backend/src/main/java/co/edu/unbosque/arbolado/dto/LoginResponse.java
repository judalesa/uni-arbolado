package co.edu.unbosque.arbolado.dto;

import java.time.Instant;

/**
 * Respuesta de un inicio de sesión correcto.
 *
 * @param token    JWT firmado
 * @param tipo     esquema para la cabecera {@code Authorization}; siempre {@code Bearer}
 * @param expiraEn instante en que el token vence (UTC)
 * @param usuario  datos de la cuenta autenticada
 */
public record LoginResponse(String token, String tipo, Instant expiraEn, UsuarioDto usuario) {
}
