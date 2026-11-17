package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.configuration.PropiedadesApp;
import co.edu.unbosque.arbolado.exception.CorreoNoEnviadoException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Modo de producción: envía el código por SMTP. */
@Service
@ConditionalOnProperty(prefix = "app.correo", name = "modo", havingValue = "smtp")
public class SmtpCorreoService implements CorreoService {

	/** Cliente SMTP configurado con {@code spring.mail.*}. */
	private final JavaMailSender remitente;

	/** Dirección que aparece como remitente. */
	private final String desde;

	/**
	 * Crea el servicio.
	 *
	 * @param remitente   cliente SMTP
	 * @param propiedades configuración de la aplicación
	 */
	public SmtpCorreoService(JavaMailSender remitente, PropiedadesApp propiedades) {
		this.remitente = remitente;
		this.desde = propiedades.correo().remitente();
	}

	/**
	 * Envía el correo con el código y su vigencia.
	 *
	 * @param destino correo del destinatario
	 * @param codigo  código de 6 dígitos
	 * @throws CorreoNoEnviadoException si el servidor SMTP rechaza o no recibe el mensaje
	 */
	@Override
	public void enviarCodigo(String destino, String codigo) {
		SimpleMailMessage mensaje = new SimpleMailMessage();
		mensaje.setFrom(desde);
		mensaje.setTo(destino);
		mensaje.setSubject("Tu código de verificación - Universidad El Arbolado");
		mensaje.setText("Tu código de verificación es " + codigo + ".\n\n"
				+ "Vence en 15 minutos. Si no solicitaste este registro, ignora este mensaje.");
		try {
			remitente.send(mensaje);
		} catch (MailException e) {
			throw new CorreoNoEnviadoException("No se pudo enviar el correo de verificación", e);
		}
	}
}
