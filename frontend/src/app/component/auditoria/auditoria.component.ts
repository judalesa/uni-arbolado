import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  AccionAuditoria,
  AuditoriaDto,
  FiltroAuditoria,
  ResultadoAuditoria,
} from '../../model/auditoria.model';
import { AuditoriaService } from '../../service/auditoria.service';
import { mensajesDe } from '../../util/errores.util';
import { aInstante, formatearInstante } from '../../util/fechas.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import { EtiquetaEstadoComponent } from '../etiqueta-estado/etiqueta-estado.component';
import { ColumnaTabla, PaginacionTabla, TablaComponent } from '../tabla/tabla.component';

/** Registros por página. */
const TAMANO_PAGINA = 20;

/** Nombre legible de cada acción, en el orden del filtro. */
export const NOMBRE_ACCION: Record<AccionAuditoria, string> = {
  INICIO_SESION: 'Inicio de sesión',
  CIERRE_SESION: 'Cierre de sesión',
  REGISTRO_ESTUDIANTE: 'Registro de estudiante',
  ENVIO_CODIGO: 'Envío de código',
  REENVIO_CODIGO: 'Reenvío de código',
  VERIFICACION_CODIGO: 'Verificación de código',
  CREACION_PROFESOR: 'Creación de profesor',
  ACTUALIZACION_USUARIO: 'Actualización de usuario',
  CREACION_PERIODO: 'Creación de periodo',
  MODIFICACION_PERIODO: 'Modificación de periodo',
  CREACION_GRUPO: 'Creación de grupo',
  MODIFICACION_GRUPO: 'Modificación de grupo',
  INSCRIPCION: 'Inscripción',
  REGISTRO_NOTA: 'Registro de nota',
};

/** Filtros tal como los escribe el administrador (fechas en hora local). */
export interface FiltrosFormulario {
  /** Usuario. */
  usuarioId: number | null;
  /** Acción. */
  accion: AccionAuditoria | null;
  /** Resultado. */
  resultado: ResultadoAuditoria | null;
  /** Desde, en hora local (`AAAA-MM-DDTHH:mm`). */
  desde: string;
  /** Hasta, en hora local. */
  hasta: string;
}

/**
 * Filtros vacíos.
 *
 * @return filtros sin valores
 */
const filtrosVacios = (): FiltrosFormulario => ({
  usuarioId: null,
  accion: null,
  resultado: null,
  desde: '',
  hasta: '',
});

/**
 * Convierte los filtros del formulario en los de la API: las fechas locales pasan a instantes con zona.
 *
 * @param filtros filtros del formulario
 * @return filtros para la consulta
 */
export const aFiltroApi = (filtros: FiltrosFormulario): FiltroAuditoria => ({
  usuarioId: filtros.usuarioId,
  accion: filtros.accion,
  resultado: filtros.resultado,
  desde: aInstante(filtros.desde),
  hasta: aInstante(filtros.hasta),
});

/**
 * Texto del usuario de un registro: correo e id, solo el id, o "Anónimo" si la acción no tuvo sesión.
 *
 * @param registro registro de auditoría
 * @return texto de la celda
 */
export const textoUsuario = (registro: AuditoriaDto): string => {
  if (registro.usuarioId === null || registro.usuarioId === undefined) {
    return registro.usuarioCorreo ?? 'Anónimo';
  }
  return registro.usuarioCorreo
    ? `${registro.usuarioCorreo} (id ${registro.usuarioId})`
    : `id ${registro.usuarioId}`;
};

/**
 * Consulta de la auditoría (`/admin/auditoria`): filtros por usuario, acción, resultado y rango de
 * fechas, con paginación. Es de solo lectura: la auditoría no se edita ni se borra (RN-30).
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-auditoria',
  /** Dependencias de la plantilla. */
  imports: [FormsModule, TablaComponent, AlertaErroresComponent, EtiquetaEstadoComponent],
  /** Plantilla HTML. */
  templateUrl: './auditoria.component.html',
})
export class AuditoriaComponent implements OnInit {
  /** Servicio de auditoría. */
  private readonly servicio = inject(AuditoriaService);

  /** Acciones del filtro. */
  protected readonly opcionesAccion = Object.entries(NOMBRE_ACCION) as [AccionAuditoria, string][];

  /** Columnas de la tabla; el resultado va en la columna de la etiqueta. */
  protected readonly columnas: ColumnaTabla<AuditoriaDto>[] = [
    {
      titulo: 'Fecha y hora',
      valor: (registro) => formatearInstante(registro.fechaHora),
      clase: 'text-nowrap',
    },
    { titulo: 'Usuario', valor: textoUsuario },
    { titulo: 'Acción', valor: (registro) => NOMBRE_ACCION[registro.accion] ?? registro.accion },
    { clave: 'entidad', titulo: 'Entidad' },
    { clave: 'idRecurso', titulo: 'Recurso', clase: 'text-center' },
    { clave: 'informacionAdicional', titulo: 'Detalle', clase: 'small' },
  ];

  /** Filtros que escribe el administrador. */
  protected filtros: FiltrosFormulario = filtrosVacios();

  /** Filtros con los que se hizo la última consulta; la paginación los conserva. */
  private filtroAplicado: FiltroAuditoria = {};

  /** Registros de la página actual. */
  protected readonly registros = signal<AuditoriaDto[]>([]);

  /** Datos de paginación de la tabla. */
  protected readonly paginacion = signal<PaginacionTabla | null>(null);

  /** Indica que se está cargando. */
  protected readonly cargando = signal(true);

  /** Mensajes de error. */
  protected readonly errores = signal<string[]>([]);

  /** Carga la primera página sin filtros. */
  ngOnInit(): void {
    this.cargar(0);
  }

  /** Aplica los filtros desde la primera página; un rango invertido no se consulta. */
  protected filtrar(): void {
    const { desde, hasta } = this.filtros;
    if (desde && hasta && desde > hasta) {
      this.errores.set(['La fecha "desde" debe ser anterior a la fecha "hasta".']);
      return;
    }
    this.filtroAplicado = aFiltroApi(this.filtros);
    this.cargar(0);
  }

  /** Borra los filtros y vuelve a consultar todo. */
  protected limpiar(): void {
    this.filtros = filtrosVacios();
    this.filtrar();
  }

  /**
   * Carga una página con los filtros vigentes.
   *
   * @param pagina número de página desde 0
   */
  protected cargar(pagina: number): void {
    this.cargando.set(true);
    this.errores.set([]);
    this.servicio.consultar(this.filtroAplicado, pagina, TAMANO_PAGINA).subscribe({
      next: (resultado) => {
        this.registros.set(resultado.contenido);
        this.paginacion.set({
          pagina: resultado.pagina,
          totalPaginas: resultado.totalPaginas,
          totalElementos: resultado.totalElementos,
        });
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }
}
