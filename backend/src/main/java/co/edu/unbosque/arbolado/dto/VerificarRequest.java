package co.edu.unbosque.arbolado.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Correo y código para activar una cuenta. */
public class VerificarRequest {

	/** Correo de la cuenta. */
	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Debe ser un correo válido")
	@Size(max = 120, message = "No puede superar 120 caracteres")
	private String correo;

	/** Código de 6 dígitos recibido por correo. */
	@NotBlank(message = "El código es obligatorio")
	@Pattern(regexp = "^[0-9]{6}$", message = "Debe tener exactamente 6 dígitos")
	private String codigo;

	/** Constructor sin argumentos que exige Jackson. */
	public VerificarRequest() {
	}

	/** @return correo de la cuenta */
	public String getCorreo() {
		return correo;
	}

	/** @param correo correo de la cuenta */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/** @return código de 6 dígitos */
	public String getCodigo() {
		return codigo;
	}

	/** @param codigo código de 6 dígitos */
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	/**
	 * Representación que omite el código para que no llegue a un registro.
	 *
	 * @return texto con el correo
	 */
	@Override
	public String toString() {
		return "VerificarRequest{correo=" + correo + "}";
	}
}
