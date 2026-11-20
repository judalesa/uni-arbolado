package co.edu.unbosque.arbolado.model;

/** Lista cerrada de eventos auditables. */
public enum AccionAuditoria {
	/** Inicio de sesión, exitoso o fallido. */
	INICIO_SESION,
	/** Cierre de sesión. */
	CIERRE_SESION,
	/** Registro de un estudiante. */
	REGISTRO_ESTUDIANTE,
	/** Creación de un profesor por el administrador. */
	CREACION_PROFESOR,
	/** Envío del código de verificación. */
	ENVIO_CODIGO,
	/** Reenvío del código de verificación. */
	REENVIO_CODIGO,
	/** Verificación del código, exitosa o fallida. */
	VERIFICACION_CODIGO,
	/** Actualización de datos de usuario (se conserva en la lista aunque no ocurra). */
	ACTUALIZACION_USUARIO,
	/** Creación de un periodo académico. */
	CREACION_PERIODO,
	/** Modificación de un periodo académico. */
	MODIFICACION_PERIODO,
	/** Creación de un grupo. */
	CREACION_GRUPO,
	/** Modificación de un grupo. */
	MODIFICACION_GRUPO,
	/** Inscripción a un grupo, exitosa o rechazada. */
	INSCRIPCION,
	/** Registro de una nota. */
	REGISTRO_NOTA,
	/** Modificación de una nota (se conserva en la lista aunque no ocurra). */
	MODIFICACION_NOTA
}
