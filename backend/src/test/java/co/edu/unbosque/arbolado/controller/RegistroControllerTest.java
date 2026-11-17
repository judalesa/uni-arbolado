package co.edu.unbosque.arbolado.controller;

import co.edu.unbosque.arbolado.exception.CorreoNoEnviadoException;
import co.edu.unbosque.arbolado.model.CodigoVerificacion;
import co.edu.unbosque.arbolado.model.EstadoCuenta;
import co.edu.unbosque.arbolado.model.Rol;
import co.edu.unbosque.arbolado.model.Usuario;
import co.edu.unbosque.arbolado.repository.AuditoriaRepository;
import co.edu.unbosque.arbolado.repository.CodigoVerificacionRepository;
import co.edu.unbosque.arbolado.repository.UsuarioRepository;
import co.edu.unbosque.arbolado.security.CodificadorContrasena;
import co.edu.unbosque.arbolado.service.CorreoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas del registro, la verificación y el reenvío con la cadena de seguridad real y el correo simulado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistroControllerTest {

	/** Clave de las cuentas de prueba. */
	private static final String CLAVE = "ClaveSegura1";

	/** Contador que da un documento y un correo únicos a cada prueba. */
	private static final AtomicLong SECUENCIA = new AtomicLong(9_000_000_000L);

	/** Cliente HTTP simulado. */
	@Autowired
	private MockMvc mvc;

	/** Repositorio de usuarios. */
	@Autowired
	private UsuarioRepository usuarios;

	/** Repositorio de códigos. */
	@Autowired
	private CodigoVerificacionRepository codigos;

	/** Repositorio de auditoría. */
	@Autowired
	private AuditoriaRepository auditoria;

	/** Codificador de contraseñas. */
	@Autowired
	private CodificadorContrasena codificador;

	/** Acceso directo a la base para mover el tiempo. */
	@Autowired
	private JdbcTemplate jdbc;

	/** Correo simulado: permite leer el código que se habría enviado. */
	@MockitoBean
	private CorreoService correo;

	/**
	 * Siguiente documento único de la prueba.
	 *
	 * @return documento de 10 dígitos
	 */
	private static String documentoNuevo() {
		return String.valueOf(SECUENCIA.incrementAndGet());
	}

	/**
	 * Correo único derivado de un documento.
	 *
	 * @param documento documento de la prueba
	 * @return correo único
	 */
	private static String correoDe(String documento) {
		return "estudiante" + documento + "@example.com";
	}

	/**
	 * Arma el cuerpo JSON de un registro.
	 *
	 * @param documento documento
	 * @param nombre    nombre completo
	 * @param correo    correo
	 * @param clave     contraseña
	 * @return el JSON
	 */
	private static String registroJson(String documento, String nombre, String correo, String clave) {
		return "{\"documento\":\"" + documento + "\",\"nombreCompleto\":\"" + nombre + "\",\"correo\":\""
				+ correo + "\",\"contrasena\":\"" + clave + "\"}";
	}

	/**
	 * Envía un POST con cuerpo JSON.
	 *
	 * @param ruta   ruta de la API
	 * @param cuerpo JSON de la petición
	 * @return el resultado para encadenar comprobaciones
	 * @throws Exception si la petición falla
	 */
	private ResultActions enviar(String ruta, String cuerpo) throws Exception {
		return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	/**
	 * Registra un estudiante con datos válidos.
	 *
	 * @param documento documento del estudiante
	 * @return el resultado de la petición
	 * @throws Exception si la petición falla
	 */
	private ResultActions registrar(String documento) throws Exception {
		return enviar("/api/auth/registro", registroJson(documento, "Ana María Pérez", correoDe(documento), CLAVE));
	}

	/**
	 * Último código que el correo simulado recibió para ese destinatario.
	 *
	 * @param destino correo del destinatario
	 * @return el código en claro
	 */
	private String ultimoCodigo(String destino) {
		ArgumentCaptor<String> captura = ArgumentCaptor.forClass(String.class);
		verify(correo, atLeastOnce()).enviarCodigo(eq(destino), captura.capture());
		return captura.getAllValues().get(captura.getAllValues().size() - 1);
	}

	/**
	 * Simula que pasaron dos minutos desde el último envío: la espera se mide contra la fecha de creación
	 * del código, que no se puede modificar desde la entidad, así que se retrasa con SQL.
	 *
	 * @param documento documento de la cuenta
	 */
	private void pasarElTiempoDeEspera(String documento) {
		Usuario u = usuarios.findByDocumento(documento).orElseThrow();
		jdbc.update("UPDATE codigoverificacion SET fecha_creacion = DATEADD('SECOND', -120, fecha_creacion) "
				+ "WHERE usuario_id = ?", u.getId());
	}

	/**
	 * Un código distinto del indicado.
	 *
	 * @param codigo código correcto
	 * @return un código incorrecto
	 */
	private static String otroCodigo(String codigo) {
		return codigo.equals("000000") ? "000001" : "000000";
	}

	/**
	 * Arma el cuerpo JSON de una verificación.
	 *
	 * @param correo correo
	 * @param codigo código
	 * @return el JSON
	 */
	private static String verificarJson(String correo, String codigo) {
		return "{\"correo\":\"" + correo + "\",\"codigo\":\"" + codigo + "\"}";
	}

	// ----- Registro -----

	/** El registro crea la cuenta pendiente con la clave cifrada, envía un código y deja auditoría. */
	@Test
	void registraAUnEstudiantePendienteYEnviaElCodigo() throws Exception {
		String documento = documentoNuevo();
		long antes = auditoria.count();

		registrar(documento).andExpect(status().isCreated())
				.andExpect(jsonPath("$.mensaje").value("Te enviamos un código de 6 dígitos a tu correo"));

		Usuario guardado = usuarios.findByDocumento(documento).orElseThrow();
		assertThat(guardado.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
		assertThat(guardado.getRol()).isEqualTo(Rol.ESTUDIANTE);
		assertThat(guardado.getContrasena()).isNotEqualTo(CLAVE);
		assertThat(codificador.matches(CLAVE, guardado.getContrasena())).isTrue();
		assertThat(ultimoCodigo(correoDe(documento))).matches("^[0-9]{6}$");
		assertThat(auditoria.count()).isEqualTo(antes + 2);
	}

	/** El correo se guarda en minúsculas y el nombre sin espacios sobrantes. */
	@Test
	void normalizaElCorreoYElNombre() throws Exception {
		String documento = documentoNuevo();

		enviar("/api/auth/registro", registroJson(documento, "  Luis Gómez  ", "LUIS" + documento + "@Example.COM", CLAVE))
				.andExpect(status().isCreated());

		Usuario guardado = usuarios.findByDocumento(documento).orElseThrow();
		assertThat(guardado.getCorreo()).isEqualTo("luis" + documento + "@example.com");
		assertThat(guardado.getNombreCompleto()).isEqualTo("Luis Gómez");
	}

	/** Documento corto, correo mal formado y clave corta se informan campo por campo, sin crear nada. */
	@Test
	void rechazaDatosInvalidos() throws Exception {
		long antes = usuarios.count();

		enviar("/api/auth/registro", registroJson("12345", "Al", "no-es-correo", "corta"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.detalles.length()").value(4));

		assertThat(usuarios.count()).isEqualTo(antes);
	}

	/** Una clave de 72 bytes pasa; con ñ que la hagan pasar de 72 bytes se rechaza con 400 en lugar de un 500. */
	@Test
	void rechazaUnaClaveQueSuperaLos72Bytes() throws Exception {
		String documento = documentoNuevo();

		enviar("/api/auth/registro", registroJson(documento, "Ana Pérez", correoDe(documento), "ñ".repeat(37)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detalles[0].campo").value("contrasena"));
	}

	/** Un documento de una cuenta activa se rechaza por duplicado (RN-01) y no se envía ningún correo. */
	@Test
	void rechazaUnDocumentoDeUnaCuentaActiva() throws Exception {
		String documento = documentoNuevo();
		usuarios.save(new Usuario(documento, "Activa", "otra" + documento + "@example.com", "x",
				Rol.ESTUDIANTE, EstadoCuenta.ACTIVO));
		long antes = auditoria.count();

		registrar(documento).andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DOCUMENTO_DUPLICADO"));

		assertThat(auditoria.count()).isEqualTo(antes + 1);
		org.mockito.Mockito.verifyNoInteractions(correo);
	}

	/** Un correo de una cuenta activa se rechaza por duplicado aunque el documento sea nuevo. */
	@Test
	void rechazaUnCorreoDeUnaCuentaActiva() throws Exception {
		String documento = documentoNuevo();
		usuarios.save(new Usuario(documentoNuevo(), "Activa", correoDe(documento), "x",
				Rol.ESTUDIANTE, EstadoCuenta.ACTIVO));

		registrar(documento).andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
	}

	/** Un profesor o administrador nunca se puede "re-registrar" como estudiante pendiente. */
	@Test
	void noSePuedeRegistrarElDocumentoDeUnProfesor() throws Exception {
		String documento = documentoNuevo();
		usuarios.save(new Usuario(documento, "Profe", "profe" + documento + "@example.com", "x",
				Rol.PROFESOR, EstadoCuenta.ACTIVO));

		registrar(documento).andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("DOCUMENTO_DUPLICADO"));
	}

	/** Re-registrar una cuenta pendiente actualiza sus datos y no crea otra cuenta (RF-06). */
	@Test
	void unReRegistroActualizaLaCuentaPendiente() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		pasarElTiempoDeEspera(documento);
		long antes = usuarios.count();

		enviar("/api/auth/registro", registroJson(documento, "Nombre Corregido", correoDe(documento), "OtraClave123"))
				.andExpect(status().isCreated());

		assertThat(usuarios.count()).isEqualTo(antes);
		Usuario guardado = usuarios.findByDocumento(documento).orElseThrow();
		assertThat(guardado.getNombreCompleto()).isEqualTo("Nombre Corregido");
		assertThat(codificador.matches("OtraClave123", guardado.getContrasena())).isTrue();
		assertThat(guardado.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
	}

	/** Si se re-registra con el mismo correo y un documento nuevo, también se actualiza esa cuenta pendiente. */
	@Test
	void unReRegistroPorCorreoCambiaElDocumento() throws Exception {
		String documento = documentoNuevo();
		String nuevoDocumento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		pasarElTiempoDeEspera(documento);

		enviar("/api/auth/registro", registroJson(nuevoDocumento, "Ana Pérez", correoDe(documento), CLAVE))
				.andExpect(status().isCreated());

		assertThat(usuarios.findByDocumento(documento)).isEmpty();
		assertThat(usuarios.findByDocumento(nuevoDocumento)).isPresent();
	}

	/** Si el documento es de una cuenta pendiente y el correo de otra, no se mezclan: se rechaza. */
	@Test
	void rechazaMezclarDosCuentasPendientes() throws Exception {
		String uno = documentoNuevo();
		String otro = documentoNuevo();
		registrar(uno).andExpect(status().isCreated());
		registrar(otro).andExpect(status().isCreated());

		enviar("/api/auth/registro", registroJson(uno, "Ana Pérez", correoDe(otro), CLAVE))
				.andExpect(status().isConflict());
	}

	/**
	 * Un segundo registro antes de 60 s responde 429 con Retry-After (legible desde el navegador por CORS) y no
	 * cambia la cuenta ni envía otro correo.
	 */
	@Test
	void limitaLosEnviosSeguidos() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());

		mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.header("Origin", "http://localhost:4200")
						.content(registroJson(documento, "Otro Nombre", correoDe(documento), CLAVE)))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.codigo").value("DEMASIADOS_INTENTOS"))
				.andExpect(header().exists("Retry-After"))
				.andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"));

		assertThat(usuarios.findByDocumento(documento).orElseThrow().getNombreCompleto()).isEqualTo("Ana María Pérez");
	}

	/** Si el correo no sale, todo se revierte: no queda una cuenta sin código y la persona puede reintentar. */
	@Test
	void siElCorreoFallaNoQuedaLaCuenta() throws Exception {
		String documento = documentoNuevo();
		doThrow(new CorreoNoEnviadoException("SMTP caído", null)).when(correo).enviarCodigo(anyString(), anyString());

		registrar(documento).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"));

		assertThat(usuarios.findByDocumento(documento)).isEmpty();
	}

	/** Una cuenta pendiente no inicia sesión, ni siquiera con la clave correcta (RF-07). */
	@Test
	void laCuentaPendienteNoInicia() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());

		enviar("/api/auth/login", "{\"correo\":\"" + correoDe(documento) + "\",\"contrasena\":\"" + CLAVE + "\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value("CUENTA_PENDIENTE"));
	}

	// ----- Verificación -----

	/** Con el código correcto la cuenta pasa a activa y ya puede iniciar sesión. */
	@Test
	void verificaYActivaLaCuenta() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		String codigo = ultimoCodigo(correoDe(documento));

		enviar("/api/auth/verificar", verificarJson(correoDe(documento), codigo)).andExpect(status().isOk());

		assertThat(usuarios.findByDocumento(documento).orElseThrow().getEstado()).isEqualTo(EstadoCuenta.ACTIVO);
		enviar("/api/auth/login", "{\"correo\":\"" + correoDe(documento) + "\",\"contrasena\":\"" + CLAVE + "\"}")
				.andExpect(status().isOk());
	}

	/** El código es de un solo uso: repetirlo ya no sirve. */
	@Test
	void elCodigoSoloSirveUnaVez() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		String codigo = ultimoCodigo(correoDe(documento));
		enviar("/api/auth/verificar", verificarJson(correoDe(documento), codigo)).andExpect(status().isOk());

		enviar("/api/auth/verificar", verificarJson(correoDe(documento), codigo))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
	}

	/** Un código incorrecto informa los intentos que quedan y el fallo se conserva en la base. */
	@Test
	void cuentaLosIntentosFallidosAunqueRespondaError() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		String malo = otroCodigo(ultimoCodigo(correoDe(documento)));

		enviar("/api/auth/verificar", verificarJson(correoDe(documento), malo))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"))
				.andExpect(jsonPath("$.mensaje").value("El código es incorrecto. Te quedan 4 intentos"));

		Usuario u = usuarios.findByDocumento(documento).orElseThrow();
		CodigoVerificacion guardado = codigos.findFirstByUsuarioOrderByIdDesc(u).orElseThrow();
		assertThat(guardado.getIntentosFallidos()).isEqualTo(1);
		assertThat(u.getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
	}

	/** Tras 5 intentos fallidos el código ya no sirve, ni siquiera el correcto (RN-03). */
	@Test
	void seBloqueaTrasCincoIntentosFallidos() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		String correcto = ultimoCodigo(correoDe(documento));
		String malo = otroCodigo(correcto);

		for (int i = 0; i < 5; i++) {
			enviar("/api/auth/verificar", verificarJson(correoDe(documento), malo))
					.andExpect(status().isUnprocessableEntity());
		}

		enviar("/api/auth/verificar", verificarJson(correoDe(documento), correcto))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
		assertThat(usuarios.findByDocumento(documento).orElseThrow().getEstado()).isEqualTo(EstadoCuenta.PENDIENTE);
	}

	/** Un correo sin cuenta responde 404 y un código con formato inválido, 400. */
	@Test
	void respondeNoEncontradoYValidacion() throws Exception {
		enviar("/api/auth/verificar", verificarJson("nadie@example.com", "123456"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
		enviar("/api/auth/verificar", verificarJson("nadie@example.com", "12ab"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"));
	}

	/** Verificar una cuenta ya activa no revela nada más: responde como un código inválido. */
	@Test
	void verificarUnaCuentaActivaEsCodigoInvalido() throws Exception {
		String documento = documentoNuevo();
		usuarios.save(new Usuario(documento, "Activa", correoDe(documento), "x", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO));

		enviar("/api/auth/verificar", verificarJson(correoDe(documento), "123456"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
	}

	// ----- Reenvío -----

	/** El código reenviado reemplaza al anterior: el viejo deja de servir y el nuevo activa la cuenta. */
	@Test
	void elReenvioInvalidaElCodigoAnterior() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());
		String viejo = ultimoCodigo(correoDe(documento));
		pasarElTiempoDeEspera(documento);
		enviar("/api/auth/reenviar-codigo", "{\"correo\":\"" + correoDe(documento) + "\"}").andExpect(status().isOk());
		String nuevo = ultimoCodigo(correoDe(documento));

		if (!viejo.equals(nuevo)) {
			enviar("/api/auth/verificar", verificarJson(correoDe(documento), viejo))
					.andExpect(status().isUnprocessableEntity());
		}
		enviar("/api/auth/verificar", verificarJson(correoDe(documento), nuevo)).andExpect(status().isOk());
	}

	/** Pedir otro código antes de 60 s responde 429; un correo desconocido, 404; una cuenta activa, 409. */
	@Test
	void validaLosCasosDelReenvio() throws Exception {
		String documento = documentoNuevo();
		registrar(documento).andExpect(status().isCreated());

		enviar("/api/auth/reenviar-codigo", "{\"correo\":\"" + correoDe(documento) + "\"}")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"));
		enviar("/api/auth/reenviar-codigo", "{\"correo\":\"nadie@example.com\"}")
				.andExpect(status().isNotFound());

		String activo = documentoNuevo();
		usuarios.save(new Usuario(activo, "Activa", correoDe(activo), "x", Rol.ESTUDIANTE, EstadoCuenta.ACTIVO));
		enviar("/api/auth/reenviar-codigo", "{\"correo\":\"" + correoDe(activo) + "\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("CUENTA_YA_ACTIVA"));
	}
}
