package co.edu.unbosque.arbolado.exception;

import co.edu.unbosque.arbolado.dto.Motivo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador SOLO de pruebas (vive en src/test): lanza cada tipo de error para comprobar el formato
 * de respuesta de {@link GlobalExceptionHandler}. No existe en el WAR.
 */
@RestController
@RequestMapping("/api/prueba")
class ControladorDePrueba {

	/** Cuerpo de ejemplo con validaciones. */
	record Peticion(@NotBlank String nombre, @Email @NotBlank String correo) {
	}

	/** Lanza un 404. */
	@GetMapping("/no-encontrado")
	void noEncontrado() {
		throw NoEncontradoException.de("La asignatura", 7);
	}

	/** Lanza un 409. */
	@GetMapping("/conflicto")
	void conflicto() {
		throw new ConflictoException(CodigoError.DOCUMENTO_DUPLICADO, "Ya existe una cuenta con ese documento");
	}

	/** Lanza un 422 con dos motivos. */
	@GetMapping("/regla")
	void regla() {
		throw new ReglaNegocioException("No se pudo realizar la inscripción", List.of(
				Motivo.de(CodigoMotivo.CREDITOS_EXCEDIDOS, "Superaría el máximo de 12 créditos"),
				Motivo.de(CodigoMotivo.HORARIO_TRASLAPADO, "Se cruza con otra asignatura")));
	}

	/** Lanza un 429. */
	@GetMapping("/demasiados")
	void demasiados() {
		throw new DemasiadosIntentosException("Espera antes de pedir otro código", 60);
	}

	/** Lanza un error no previsto para comprobar que no se filtra su detalle. */
	@GetMapping("/inesperado")
	void inesperado() {
		throw new IllegalStateException("detalle interno que NO debe llegar al cliente: clave=abc123");
	}

	/** Solo accesible para el rol ADMINISTRADOR. */
	@GetMapping("/solo-admin")
	@PreAuthorize("hasRole('ADMINISTRADOR')")
	void soloAdmin() {
		// nada: lo importante es la restricción de rol
	}

	/** Recibe un número en la ruta para probar la conversión de tipos. */
	@GetMapping("/numero/{id}")
	void numero(@PathVariable Long id) {
		// nada: se prueba la conversión del parámetro
	}

	/** Recibe un cuerpo con validaciones. */
	@PostMapping("/validar")
	void validar(@Valid @RequestBody Peticion peticion) {
		// nada: se prueba la validación del cuerpo
	}
}
