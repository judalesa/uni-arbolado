package co.edu.unbosque.arbolado.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Modo de desarrollo y pruebas: imprime el código en el registro en lugar de enviarlo. */
@Service
@ConditionalOnProperty(prefix = "app.correo", name = "modo", havingValue = "log")
public class LogCorreoService implements CorreoService {

	/** Registro de la aplicación. */
	private static final Logger LOG = LoggerFactory.getLogger(LogCorreoService.class);

	/**
	 * Escribe el código en el registro.
	 *
	 * @param destino correo del destinatario
	 * @param codigo  código de 6 dígitos
	 */
	@Override
	public void enviarCodigo(String destino, String codigo) {
		LOG.info("[CORREO SIMULADO] Código de verificación para {}: {}", destino, codigo);
	}
}
