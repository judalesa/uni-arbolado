package co.edu.unbosque.arbolado.exception;

import co.edu.unbosque.arbolado.dto.ErrorResponse;
import co.edu.unbosque.arbolado.dto.Motivo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas unitarias del catálogo de errores y de las excepciones propias. */
class ExcepcionesTest {

	/** Todo código del catálogo debe tener un estado HTTP de error (400 a 599). */
	@Test
	void todoCodigoDeErrorTieneUnEstadoHttpValido() {
		for (CodigoError codigo : CodigoError.values()) {
			assertThat(codigo.estado()).as(codigo.name()).isBetween(400, 599);
		}
	}

	/** Cada subclase solo acepta códigos de su familia HTTP; mezclarlos lanza {@link IllegalArgumentException}. */
	@Test
	void cadaSubclaseAceptaSoloCodigosDeSuFamiliaHttp() {
		assertThat(new ConflictoException(CodigoError.DOCUMENTO_DUPLICADO, "x").getCodigo())
				.isEqualTo(CodigoError.DOCUMENTO_DUPLICADO);
		assertThatThrownBy(() -> new ConflictoException(CodigoError.NO_ENCONTRADO, "x"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new NoAutenticadoException(CodigoError.ACCESO_DENEGADO, "x"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new AccesoDenegadoException(CodigoError.NO_AUTENTICADO, "x"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ReglaNegocioException(CodigoError.VALIDACION, "x"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	/** La fábrica {@code NoEncontradoException.de} arma un mensaje estándar con el recurso y su identificador. */
	@Test
	void noEncontradoConstruyeUnMensajeEstandar() {
		NoEncontradoException ex = NoEncontradoException.de("La asignatura", 99L);
		assertThat(ex.getCodigo()).isEqualTo(CodigoError.NO_ENCONTRADO);
		assertThat(ex.getMessage()).isEqualTo("La asignatura con identificador 99 no existe");
	}

	/** La excepción de regla de negocio conserva todos los motivos y su lista no se puede alterar desde fuera. */
	@Test
	void reglaDeNegocioGuardaTodosLosMotivosYLosProtegeDeCambios() {
		List<Motivo> origen = new ArrayList<>(List.of(
				Motivo.de(CodigoMotivo.CREDITOS_EXCEDIDOS, "Superaría 12 créditos"),
				Motivo.de(CodigoMotivo.GRUPO_LLENO, "El grupo no tiene cupos")));
		ReglaNegocioException ex = new ReglaNegocioException("No se pudo inscribir", origen);

		origen.clear(); // la excepción no debe verse afectada
		assertThat(ex.getCodigo()).isEqualTo(CodigoError.REGLA_NEGOCIO);
		assertThat(ex.getMotivos()).extracting(Motivo::codigo).containsExactly("CREDITOS_EXCEDIDOS", "GRUPO_LLENO");
		assertThatThrownBy(() -> ex.getMotivos().add(new Motivo("X", "x")))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	/** La excepción de demasiados intentos informa los segundos de espera. */
	@Test
	void demasiadosIntentosIndicaLaEspera() {
		DemasiadosIntentosException ex = new DemasiadosIntentosException("Espera un momento", 60);
		assertThat(ex.getCodigo()).isEqualTo(CodigoError.DEMASIADOS_INTENTOS);
		assertThat(ex.getSegundosEspera()).isEqualTo(60);
	}

	/** La respuesta de error toma su estado del código y deja en {@code null} las listas que no aplican. */
	@Test
	void laRespuestaDeErrorTomaElEstadoDelCodigoYOmiteListasVacias() {
		ErrorResponse r = ErrorResponse.de(CodigoError.CORREO_DUPLICADO, "Ya existe", "/api/auth/registro");
		assertThat(r.status()).isEqualTo(409);
		assertThat(r.codigo()).isEqualTo("CORREO_DUPLICADO");
		assertThat(r.timestamp()).isNotNull();
		assertThat(r.detalles()).isNull();
		assertThat(r.motivos()).isNull();
	}
}
