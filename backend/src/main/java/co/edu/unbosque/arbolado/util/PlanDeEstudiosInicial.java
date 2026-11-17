package co.edu.unbosque.arbolado.util;

import java.util.List;

/** Datos del plan de estudios (34 asignaturas) que se precargan al arrancar. */
public final class PlanDeEstudiosInicial {

	/**
	 * Una asignatura del plan con los códigos de sus prerrequisitos.
	 *
	 * @param codigo         código único
	 * @param nombre         nombre
	 * @param semestre       semestre del plan
	 * @param creditos       créditos
	 * @param descripcion    descripción breve
	 * @param prerrequisitos códigos de las asignaturas requisito
	 */
	public record AsignaturaSemilla(String codigo, String nombre, int semestre, int creditos,
									String descripcion, List<String> prerrequisitos) {
	}

	/** Plan completo, ordenado por semestre. */
	private static final List<AsignaturaSemilla> ASIGNATURAS = List.of(
			new AsignaturaSemilla("MAT101", "Matemáticas 1", 1, 3,
					"Funciones, límites y cálculo diferencial de una variable.", List.of()),
			new AsignaturaSemilla("ING101", "Fundamentos de la Ingeniería", 1, 3,
					"Introducción a la profesión, la metodología de ingeniería y el trabajo en equipo.", List.of()),
			new AsignaturaSemilla("DSI101", "Desarrollo de Sistemas de Información 1", 1, 4,
					"Fundamentos de programación y lógica algorítmica.", List.of()),
			new AsignaturaSemilla("PSI101", "Principios de Sistemas de Información", 1, 2,
					"Conceptos básicos de los sistemas de información en las organizaciones.", List.of()),

			new AsignaturaSemilla("MAT201", "Matemáticas 2", 2, 3,
					"Cálculo integral y series.", List.of("MAT101")),
			new AsignaturaSemilla("ALG201", "Álgebra Lineal", 2, 2,
					"Vectores, matrices, sistemas de ecuaciones y transformaciones lineales.", List.of("MAT101")),
			new AsignaturaSemilla("FIS201", "Física 1", 2, 2,
					"Mecánica clásica: cinemática, dinámica y energía.", List.of("MAT101")),
			new AsignaturaSemilla("DSI201", "Desarrollo de Sistemas de Información 2", 2, 4,
					"Programación orientada a objetos.", List.of("DSI101")),

			new AsignaturaSemilla("EST301", "Estadística y Probabilidad", 3, 2,
					"Probabilidad, distribuciones e inferencia estadística básica.", List.of("MAT201")),
			new AsignaturaSemilla("FIS301", "Física 2", 3, 2,
					"Electricidad, magnetismo y ondas.", List.of("FIS201", "MAT201")),
			new AsignaturaSemilla("DSI301", "Desarrollo de Sistemas de Información 3", 3, 4,
					"Desarrollo de aplicaciones con acceso a datos y servicios.", List.of("DSI201")),
			new AsignaturaSemilla("EDA301", "Estructuras de Datos", 3, 3,
					"Listas, pilas, colas, árboles, grafos y análisis de complejidad.", List.of("DSI201")),

			new AsignaturaSemilla("ESI401", "Estructuras de Sistemas", 4, 3,
					"Arquitectura de computadores y sistemas operativos.", List.of("EDA301")),
			new AsignaturaSemilla("BDA401", "Bases de Datos 1", 4, 3,
					"Modelo relacional, normalización y lenguaje SQL.", List.of("DSI201")),
			new AsignaturaSemilla("ITI401", "Infraestructura TI 1", 4, 3,
					"Fundamentos de redes y comunicaciones.", List.of("ING101")),
			new AsignaturaSemilla("PNU401", "Proyecto Núcleo 1", 4, 2,
					"Proyecto integrador de desarrollo de software de primer nivel.", List.of("DSI301", "EDA301")),

			new AsignaturaSemilla("ITI501", "Infraestructura TI 2", 5, 3,
					"Administración de servidores y servicios de red.", List.of("ITI401")),
			new AsignaturaSemilla("BDA501", "Bases de Datos 2", 5, 3,
					"Bases de datos avanzadas, transacciones y optimización de consultas.", List.of("BDA401")),
			new AsignaturaSemilla("ISW501", "Ingeniería de Software 1", 5, 4,
					"Ciclo de vida del software, requerimientos y modelado.", List.of("DSI301", "BDA401")),
			new AsignaturaSemilla("MIN501", "Modelización Informacional", 5, 2,
					"Modelado de información y de procesos de negocio.", List.of("EST301", "BDA401")),

			new AsignaturaSemilla("ITI601", "Infraestructura TI 3", 6, 3,
					"Virtualización y servicios en la nube.", List.of("ITI501")),
			new AsignaturaSemilla("ISW601", "Ingeniería de Software 2", 6, 4,
					"Diseño de arquitectura, patrones y pruebas de software.", List.of("ISW501")),
			new AsignaturaSemilla("MSI601", "Modelos y Simulación de Sistemas de Información", 6, 2,
					"Modelado y simulación de sistemas.", List.of("EST301", "MIN501")),
			new AsignaturaSemilla("PNU601", "Proyecto Núcleo 2", 6, 2,
					"Proyecto integrador de desarrollo de software de segundo nivel.", List.of("PNU401", "ISW501")),

			new AsignaturaSemilla("ISW701", "Ingeniería de Software 3", 7, 4,
					"Calidad, mantenimiento y gestión de la configuración del software.", List.of("ISW601")),
			new AsignaturaSemilla("FGP701", "Formulación y Gestión de Proyectos", 7, 2,
					"Formulación, planeación y seguimiento de proyectos.", List.of("ISW501")),
			new AsignaturaSemilla("SEG701", "Seguridad de la Información", 7, 3,
					"Principios de seguridad, criptografía y gestión de riesgos.", List.of("ITI501")),
			new AsignaturaSemilla("LDE701", "Lenguajes de Dominio Específico", 7, 2,
					"Diseño e implementación de lenguajes para dominios específicos.", List.of("EDA301", "ISW501")),

			new AsignaturaSemilla("DPI801", "Diseño de Proyectos de Ingeniería", 8, 1,
					"Diseño del proyecto de grado.", List.of("FGP701", "ISW701")),
			new AsignaturaSemilla("ECF801", "Economía y Finanzas", 8, 1,
					"Fundamentos de economía y evaluación financiera de proyectos.", List.of("MAT201")),
			new AsignaturaSemilla("SIN801", "Sistemas Inteligentes", 8, 2,
					"Introducción a la inteligencia artificial.", List.of("ALG201", "EST301", "EDA301")),
			new AsignaturaSemilla("MLE801", "Machine Learning", 8, 3,
					"Aprendizaje automático supervisado y no supervisado.", List.of("ALG201", "EST301", "LDE701")),

			new AsignaturaSemilla("PVL901", "Preparación para la Vida Laboral", 9, 2,
					"Competencias profesionales y ejercicio laboral.", List.of()),
			new AsignaturaSemilla("TGR901", "Trabajo de Grado", 9, 5,
					"Desarrollo y sustentación del trabajo de grado.", List.of("PNU601", "DPI801"))
	);

	/** Clase de utilidad: no se instancia. */
	private PlanDeEstudiosInicial() {
	}

	/**
	 * Devuelve el plan completo.
	 *
	 * @return lista inmutable de las 34 asignaturas
	 */
	public static List<AsignaturaSemilla> asignaturas() {
		return ASIGNATURAS;
	}
}
