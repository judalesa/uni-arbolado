package co.edu.unbosque.arbolado.service;

import co.edu.unbosque.arbolado.model.AccionAuditoria;
import co.edu.unbosque.arbolado.model.Auditoria;
import co.edu.unbosque.arbolado.model.Resultado;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de la auditoría: transacción independiente para los fallidos. */
@SpringBootTest
@ActiveProfiles("test")
class AuditoriaServiceTest {

	/** Servicio bajo prueba. */
	@Autowired
	private AuditoriaService servicio;

	/** Repositorio de auditoría. */
	@Autowired
	private AuditoriaRepository repositorio;

	/** Plantilla para abrir la transacción "externa" que luego se revierte. */
	private final TransactionTemplate externa;

	/**
	 * Prepara la plantilla de transacciones.
	 *
	 * @param manager administrador de transacciones de Spring
	 */
	@Autowired
	AuditoriaServiceTest(PlatformTransactionManager manager) {
		this.externa = new TransactionTemplate(manager);
	}

	/** Un evento fallido queda guardado aunque la transacción de la operación se revierta. */
	@Test
	void elFallidoPersisteAunqueLaTransaccionExternaSeRevierte() {
		long antes = repositorio.count();

		externa.executeWithoutResult(estado -> {
			servicio.registrarFallido(AccionAuditoria.INSCRIPCION, 7L, "ana@correo.com", "INSCRIPCION", "42",
					"Créditos excedidos");
			estado.setRollbackOnly();
		});

		assertThat(repositorio.count()).isEqualTo(antes + 1);
	}

	/** Un evento exitoso se revierte junto con la operación: no queda rastro de algo que no ocurrió. */
	@Test
	void elExitosoSeRevierteConLaTransaccionExterna() {
		long antes = repositorio.count();

		externa.executeWithoutResult(estado -> {
			servicio.registrarExitoso(AccionAuditoria.INSCRIPCION, 7L, "ana@correo.com", "INSCRIPCION", "43", null);
			estado.setRollbackOnly();
		});

		assertThat(repositorio.count()).isEqualTo(antes);
	}

	/** Un evento exitoso se guarda cuando la transacción de la operación se confirma. */
	@Test
	void elExitosoPersisteSiLaTransaccionSeConfirma() {
		long antes = repositorio.count();

		externa.executeWithoutResult(estado ->
				servicio.registrarExitoso(AccionAuditoria.CIERRE_SESION, 7L, "ana@correo.com", "USUARIO", "7", null));

		assertThat(repositorio.count()).isEqualTo(antes + 1);
	}

	/** Los eventos anónimos se guardan sin usuario, con el correo intentado. */
	@Test
	void unEventoAnonimoGuardaElCorreoIntentadoSinUsuario() {
		Long id = servicio.registrarFallido(AccionAuditoria.INICIO_SESION, null, "nadie@correo.com", "USUARIO", null,
				"Credenciales inválidas");

		Auditoria registro = repositorio.findById(id).orElseThrow();
		assertThat(registro.getUsuarioId()).isNull();
		assertThat(registro.getUsuarioCorreo()).isEqualTo("nadie@correo.com");
		assertThat(registro.getResultado()).isEqualTo(Resultado.FALLIDO);
		assertThat(registro.getFechaHora()).isNotNull();
	}

	/** La información adicional demasiado larga se recorta al máximo permitido. */
	@Test
	void laInformacionLargaSeRecorta() {
		Long id = servicio.registrarFallido(AccionAuditoria.VERIFICACION_CODIGO, 1L, "a@b.com", "USUARIO", "1",
				"x".repeat(AuditoriaService.MAX_INFORMACION + 500));

		assertThat(repositorio.findById(id).orElseThrow().getInformacionAdicional())
				.hasSize(AuditoriaService.MAX_INFORMACION);
	}

	/** La acción es obligatoria. */
	@Test
	void laAccionEsObligatoria() {
		assertThatThrownBy(() -> servicio.registrarFallido(null, 1L, "a@b.com", "USUARIO", "1", null))
				.isInstanceOf(NullPointerException.class);
	}
}
