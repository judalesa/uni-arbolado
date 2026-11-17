package co.edu.unbosque.arbolado.model;

/** Estado de una cuenta de usuario. */
public enum EstadoCuenta {
	/** Registrada pero con el correo sin verificar. */
	PENDIENTE,
	/** Verificada y habilitada para iniciar sesión. */
	ACTIVO
}
