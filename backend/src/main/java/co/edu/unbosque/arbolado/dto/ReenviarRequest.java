package co.edu.unbosque.arbolado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Correo de la cuenta que pide un código nuevo. */
public class ReenviarRequest {

	/** Correo de la cuenta. */
	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Debe ser un correo válido")
	@Size(max = 120, message = "No puede superar 120 caracteres")
	private String correo;

	/** Constructor sin argumentos que exige Jackson. */
	public ReenviarRequest() {
	}

	/** @return correo de la cuenta */
	public String getCorreo() {
		return correo;
	}

	/** @param correo correo de la cuenta */
	public void setCorreo(String correo) {
		this.correo = correo;
	}
}
