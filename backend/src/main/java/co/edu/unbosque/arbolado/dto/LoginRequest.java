package co.edu.unbosque.arbolado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales para iniciar sesión. */
public class LoginRequest {

	/** Correo de la cuenta. */
	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Debe ser un correo válido")
	@Size(max = 120, message = "No puede superar 120 caracteres")
	private String correo;

	/** Contraseña en texto plano; solo viaja por HTTPS y nunca se registra. */
	@NotBlank(message = "La contraseña es obligatoria")
	@Size(max = 128, message = "No puede superar 128 caracteres")
	private String contrasena;

	/** Constructor sin argumentos que exige Jackson. */
	public LoginRequest() {
	}

	/** @return correo de la cuenta */
	public String getCorreo() {
		return correo;
	}

	/** @param correo correo de la cuenta */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/** @return contraseña en texto plano */
	public String getContrasena() {
		return contrasena;
	}

	/** @param contrasena contraseña en texto plano */
	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

	/**
	 * Representación que omite la contraseña para que nunca llegue a un registro.
	 *
	 * @return texto con el correo
	 */
	@Override
	public String toString() {
		return "LoginRequest{correo=" + correo + "}";
	}
}
