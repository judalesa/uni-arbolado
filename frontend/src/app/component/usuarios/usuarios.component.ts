import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { Pagina } from '../../model/pagina.model';
import { UsuarioDto } from '../../model/usuario.model';
import { ToastService } from '../../service/toast.service';
import { RolListable, UsuarioService } from '../../service/usuario.service';
import { formatearInstante } from '../../util/fechas.util';
import { erroresPorCampo, leerError, mensajesDe } from '../../util/errores.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';
import {
  CampoFormulario,
  FormularioComponent,
  ValoresFormulario,
} from '../formulario/formulario.component';
import { ColumnaTabla, PaginacionTabla, TablaComponent } from '../tabla/tabla.component';

/** Elementos por página del listado. */
const TAMANO_PAGINA = 20;

/** Campo del formulario al que corresponde cada error de duplicado del backend. */
const CAMPO_POR_CODIGO: Record<string, string> = {
  DOCUMENTO_DUPLICADO: 'documento',
  CORREO_DUPLICADO: 'correo',
};

/** Textos de la pantalla según el rol que se lista. */
const TEXTOS: Record<RolListable, { titulo: string; icono: string; vacio: string }> = {
  ESTUDIANTE: {
    titulo: 'Estudiantes',
    icono: 'fa-user-graduate',
    vacio: 'No hay estudiantes que coincidan.',
  },
  PROFESOR: {
    titulo: 'Profesores',
    icono: 'fa-chalkboard-user',
    vacio: 'No hay profesores que coincidan.',
  },
};

/**
 * Listado de estudiantes o de profesores (`/admin/estudiantes`, `/admin/profesores`) con búsqueda y
 * paginación. El rol sale de los datos de la ruta para no duplicar la pantalla. Solo en profesores
 * aparece la creación, porque los estudiantes se registran solos.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-usuarios',
  /** Dependencias de la plantilla. */
  imports: [FormsModule, TablaComponent, FormularioComponent, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './usuarios.component.html',
})
export class UsuariosComponent implements OnInit {
  /** Servicio de usuarios. */
  private readonly servicio = inject(UsuarioService);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Rol que lista esta pantalla. */
  protected readonly rol: RolListable =
    inject(ActivatedRoute).snapshot.data['rol'] === 'PROFESOR' ? 'PROFESOR' : 'ESTUDIANTE';

  /** Textos de la pantalla. */
  protected readonly textos = TEXTOS[this.rol];

  /** Si la pantalla permite crear (solo profesores). */
  protected readonly permiteCrear = this.rol === 'PROFESOR';

  /** Columnas de la tabla. */
  protected readonly columnas: ColumnaTabla<UsuarioDto>[] = [
    { clave: 'documento', titulo: 'Documento' },
    { clave: 'nombreCompleto', titulo: 'Nombre' },
    { clave: 'correo', titulo: 'Correo' },
    { titulo: 'Estado', valor: (u) => (u.estado === 'ACTIVO' ? 'Activo' : 'Pendiente') },
    { titulo: 'Creado', valor: (u) => formatearInstante(u.fechaCreacion) },
  ];

  /** Campos del formulario de creación; las reglas son las de `CrearProfesorRequest`. */
  protected readonly campos: CampoFormulario[] = [
    {
      nombre: 'documento',
      etiqueta: 'Documento',
      tipo: 'text',
      requerido: true,
      patron: '^[0-9]{10}$',
      mensajePatron: 'El documento debe tener exactamente 10 dígitos.',
      autocompletar: 'off',
    },
    {
      nombre: 'nombreCompleto',
      etiqueta: 'Nombre completo',
      tipo: 'text',
      requerido: true,
      minimo: 3,
      maximo: 120,
      autocompletar: 'off',
    },
    {
      nombre: 'correo',
      etiqueta: 'Correo',
      tipo: 'email',
      requerido: true,
      maximo: 120,
      autocompletar: 'off',
    },
    {
      nombre: 'contrasena',
      etiqueta: 'Contraseña inicial',
      tipo: 'password',
      requerido: true,
      minimo: 8,
      maximo: 72,
      autocompletar: 'new-password',
      ayuda: 'Mínimo 8 caracteres. Entrégasela al profesor por un medio seguro.',
    },
  ];

  /** Texto de búsqueda escrito por el usuario. */
  protected busqueda = '';

  /** Texto con el que se hizo la última búsqueda; la paginación lo conserva. */
  private busquedaAplicada = '';

  /** Usuarios de la página actual. */
  protected readonly usuarios = signal<UsuarioDto[]>([]);

  /** Datos de paginación de la tabla. */
  protected readonly paginacion = signal<PaginacionTabla | null>(null);

  /** Indica que se está cargando el listado. */
  protected readonly cargando = signal(true);

  /** Si el formulario de creación está visible. */
  protected readonly creando = signal(false);

  /** Valores iniciales del formulario; un objeto nuevo lo deja en blanco. */
  protected readonly valoresIniciales = signal<ValoresFormulario>({});

  /** Indica que se está guardando. */
  protected readonly enviando = signal(false);

  /** Mensajes de error generales. */
  protected readonly errores = signal<string[]>([]);

  /** Errores por campo devueltos por el backend. */
  protected readonly erroresCampo = signal<Record<string, string>>({});

  /** Carga la primera página al abrir la pantalla. */
  ngOnInit(): void {
    this.cargar(0);
  }

  /** Busca desde la primera página con el texto escrito. */
  protected buscar(): void {
    this.busquedaAplicada = this.busqueda;
    this.cargar(0);
  }

  /** Borra la búsqueda y vuelve a listar todo. */
  protected limpiarBusqueda(): void {
    this.busqueda = '';
    this.buscar();
  }

  /**
   * Carga una página del listado con la búsqueda vigente.
   *
   * @param pagina número de página desde 0
   */
  protected cargar(pagina: number): void {
    this.cargando.set(true);
    this.servicio.listar(this.rol, this.busquedaAplicada, pagina, TAMANO_PAGINA).subscribe({
      next: (resultado: Pagina<UsuarioDto>) => {
        this.usuarios.set(resultado.contenido);
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

  /** Muestra u oculta el formulario de creación, siempre en blanco. */
  protected alternarCreacion(): void {
    this.errores.set([]);
    this.erroresCampo.set({});
    this.valoresIniciales.set({});
    this.creando.update((visible) => !visible);
  }

  /**
   * Crea el profesor y recarga el listado.
   *
   * @param valores datos del formulario
   */
  protected crear(valores: ValoresFormulario): void {
    this.enviando.set(true);
    this.errores.set([]);
    this.erroresCampo.set({});
    this.servicio
      .crearProfesor({
        documento: String(valores['documento']),
        nombreCompleto: String(valores['nombreCompleto']),
        correo: String(valores['correo']),
        contrasena: String(valores['contrasena']),
      })
      .subscribe({
        next: (profesor) => {
          this.enviando.set(false);
          this.creando.set(false);
          this.avisos.exito(`Profesor ${profesor.nombreCompleto} creado.`);
          this.cargar(0);
        },
        error: (error: unknown) => this.alFallarCreacion(error),
      });
  }

  /**
   * Un documento o correo repetido se marca en su campo; el resto se muestra como mensaje general.
   *
   * @param error error recibido
   */
  private alFallarCreacion(error: unknown): void {
    this.enviando.set(false);
    const cuerpo = leerError(error);
    const campo = CAMPO_POR_CODIGO[cuerpo?.codigo ?? ''];
    if (cuerpo && campo) {
      this.erroresCampo.set({ [campo]: cuerpo.mensaje });
      return;
    }
    this.errores.set(mensajesDe(error));
    this.erroresCampo.set(erroresPorCampo(error));
  }
}
