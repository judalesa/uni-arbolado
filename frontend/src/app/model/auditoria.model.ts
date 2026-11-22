/** Acciones que registra la auditoría (enumeración `AccionAuditoria` del backend). */
export type AccionAuditoria =
  | 'INICIO_SESION'
  | 'CIERRE_SESION'
  | 'REGISTRO_ESTUDIANTE'
  | 'CREACION_PROFESOR'
  | 'ENVIO_CODIGO'
  | 'REENVIO_CODIGO'
  | 'VERIFICACION_CODIGO'
  | 'ACTUALIZACION_USUARIO'
  | 'CREACION_PERIODO'
  | 'MODIFICACION_PERIODO'
  | 'CREACION_GRUPO'
  | 'MODIFICACION_GRUPO'
  | 'INSCRIPCION'
  | 'REGISTRO_NOTA';

/** Resultado de la acción auditada. */
export type ResultadoAuditoria = 'EXITOSO' | 'FALLIDO';

/** Registro de auditoría (`AuditoriaDto`); es de solo lectura. */
export interface AuditoriaDto {
  /** Identificador. */
  id: number;
  /** Usuario que hizo la acción, o `null` si fue anónima (por ejemplo, un login fallido). */
  usuarioId?: number | null;
  /** Correo del usuario, o `null`. */
  usuarioCorreo?: string | null;
  /** Acción registrada. */
  accion: AccionAuditoria;
  /** Instante de la acción (UTC). */
  fechaHora: string;
  /** Entidad afectada. */
  entidad?: string;
  /** Identificador del recurso afectado, o `null`. */
  idRecurso?: string | null;
  /** Resultado. */
  resultado: ResultadoAuditoria;
  /** Detalle adicional, por ejemplo el motivo de un fallo. */
  informacionAdicional?: string | null;
}

/** Filtros de la consulta de auditoría; los vacíos no se envían. */
export interface FiltroAuditoria {
  /** Usuario. */
  usuarioId?: number | null;
  /** Acción. */
  accion?: AccionAuditoria | null;
  /** Resultado. */
  resultado?: ResultadoAuditoria | null;
  /** Instante inicial, en ISO 8601 con zona. */
  desde?: string | null;
  /** Instante final, en ISO 8601 con zona. */
  hasta?: string | null;
}
