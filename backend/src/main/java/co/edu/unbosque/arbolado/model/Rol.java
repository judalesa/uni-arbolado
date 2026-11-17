package co.edu.unbosque.arbolado.model;

/** Rol de una cuenta; define qué puede hacer en el sistema. */
public enum Rol {
	/** Consulta su plan de estudios, se inscribe y revisa sus notas. */
	ESTUDIANTE,
	/** Consulta sus grupos y registra notas. */
	PROFESOR,
	/** Administra usuarios, periodos y grupos. */
	ADMINISTRADOR
}
