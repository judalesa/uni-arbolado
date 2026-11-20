package co.edu.unbosque.arbolado.dto;

import co.edu.unbosque.arbolado.util.MaximoBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para que el administrador cree un profesor. */
public class CrearProfesorRequest {

	/** Documento de exactamente 10 dígitos. */
	@NotBlank(message = "El documento es obligatorio")
	@Pattern(regexp = "^[0-9]{10}$", message = "Debe tener exactamente 10 dígitos")
	private String documento;

	/** Nombre completo. */
	@NotBlank(message = "El nombre es obligatorio")
	@Size(min = 3, max = 120, message = "Debe tener entre 3 y 120 caracteres")
	private String nombreCompleto;

	/** Correo de la cuenta. */
	@NotBlank(message = "El correo es obligatorio")
	@Email(message = "Debe ser un correo válido")
	@Size(max = 120, message = "No puede superar 120 caracteres")
	private String correo;

	/** Contraseña inicial en texto plano; solo viaja por HTTPS y nunca se registra. */
	@NotBlank(message = "La contraseña es obligatoria")
	@Size(min = 8, message = "Debe tener al menos 8 caracteres")
	@MaximoBytes(value = 72, message = "No puede superar 72 bytes (los caracteres con tilde o ñ ocupan más de uno)")
	private String contrasena;

	/** Constructor sin argumentos que exige Jackson. */
	public CrearProfesorRequest() {
	}

	/** @return documento de 10 dígitos */
	public String getDocumento() {
		return documento;
	}

	/** @param documento documento de 10 dígitos */
	public void setDocumento(String documento) {
		this.documento = documento;
	}

	/** @return nombre completo */
	public String getNombreCompleto() {
		return nombreCompleto;
	}

	/** @param nombreCompleto nombre completo */
	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}

	/** @return correo de la cuenta */
	public String getCorreo() {
		return correo;
	}

	/** @param correo correo de la cuenta */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/** @return contraseña inicial en texto plano */
	public String getContrasena() {
		return contrasena;
	}

	/** @param contrasena contraseña inicial en texto plano */
	public void setContrasena(String contrasena) {
		this.contrasena = contrasena;
	}

	/**
	 * Representación que omite la contraseña para que nunca llegue a un registro.
	 *
	 * @return texto con el documento y el correo
	 */
	@Override
	public String toString() {
		return "CrearProfesorRequest{documento=" + documento + ", correo=" + correo + "}";
	}
}
