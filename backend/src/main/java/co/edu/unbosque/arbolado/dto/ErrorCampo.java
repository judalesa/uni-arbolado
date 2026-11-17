package co.edu.unbosque.arbolado.dto;

/**
 * Error de validación asociado a un campo concreto de la petición (solo aparece en respuestas 400).
 *
 * @param campo   nombre del campo inválido, por ejemplo {@code correo}
 * @param mensaje explicación legible, por ejemplo "Debe ser un correo válido"
 */
public record ErrorCampo(String campo, String mensaje) {
}
