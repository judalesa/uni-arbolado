package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.exception.CorreoNoEnviadoException;
import co.edu.unbosque.arbolado.security.PropiedadesDePrueba;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Pruebas del envío por SMTP con un cliente simulado. */
class SmtpCorreoServiceTest {

	/** Cliente SMTP simulado. */
	private final JavaMailSender remitente = mock(JavaMailSender.class);

	/** Servicio bajo prueba. */
	private final SmtpCorreoService servicio = new SmtpCorreoService(remitente, PropiedadesDePrueba.estandar());

	/** El mensaje sale del remitente configurado, va al destinatario y trae el código y la vigencia. */
	@Test
	void enviaElCodigoAlDestinatario() {
		servicio.enviarCodigo("ana@example.com", "482913");

		ArgumentCaptor<SimpleMailMessage> captura = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(remitente).send(captura.capture());
		SimpleMailMessage mensaje = captura.getValue();
		assertThat(mensaje.getFrom()).isEqualTo("pruebas@example.com");
		assertThat(mensaje.getTo()).containsExactly("ana@example.com");
		assertThat(mensaje.getText()).contains("482913").contains("15 minutos");
	}

	/** Si el servidor SMTP falla se lanza una excepción propia, sin exponer detalles al cliente. */
	@Test
	void traduceElFalloDelServidorDeCorreo() {
		doThrow(new MailSendException("SMTP caído")).when(remitente).send(any(SimpleMailMessage.class));

		assertThatThrownBy(() -> servicio.enviarCodigo("ana@example.com", "482913"))
				.isInstanceOf(CorreoNoEnviadoException.class);
	}
}
