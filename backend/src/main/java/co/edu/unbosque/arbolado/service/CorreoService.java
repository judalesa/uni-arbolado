package co.edu.unbosque.arbolado.service;

/** Envío de los correos de la aplicación; hay una implementación por modo ({@code app.correo.modo}). */
public interface CorreoService {

	/**
	 * Envía el código de verificación.
	 *
	 * @param destino correo del destinatario
	 * @param codigo  código de 6 dígitos en claro
	 * @throws co.edu.unbosque.arbolado.exception.CorreoNoEnviadoException si el correo no se pudo entregar
	 */
	void enviarCodigo(String destino, String codigo);
}
