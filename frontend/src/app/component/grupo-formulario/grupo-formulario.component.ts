import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { AsignaturaDto } from '../../model/asignatura.model';
import { DiaSemana, GrupoDto, GrupoRequest, HorarioDto } from '../../model/grupo.model';
import { PeriodoDto } from '../../model/periodo.model';
import { UsuarioDto } from '../../model/usuario.model';
import { AsignaturaService } from '../../service/asignatura.service';
import { GrupoService } from '../../service/grupo.service';
import { PeriodoService } from '../../service/periodo.service';
import { ToastService } from '../../service/toast.service';
import { UsuarioService } from '../../service/usuario.service';
import { erroresPorCampo, mensajesDe } from '../../util/errores.util';
import { DIAS, FranjaEditable, ordenarHorarios, validarHorarios } from '../../util/horario.util';
import { AlertaErroresComponent } from '../alerta-errores/alerta-errores.component';

/** Máximo de profesores que se traen para el selector; el contrato permite hasta 100 por página. */
const MAXIMO_PROFESORES = 100;

/** Controles de una franja del formulario. */
type ControlesFranja = {
  /** Día de la semana. */
  diaSemana: FormControl<DiaSemana | ''>;
  /** Hora de inicio. */
  horaInicio: FormControl<string>;
  /** Hora de fin. */
  horaFin: FormControl<string>;
};

/**
 * Crea los controles de una franja.
 *
 * @param franja valores iniciales; vacía si se omite
 * @return grupo de controles de la franja
 */
const crearFranja = (
  franja: FranjaEditable = { diaSemana: '', horaInicio: '', horaFin: '' },
): FormGroup<ControlesFranja> =>
  new FormGroup<ControlesFranja>({
    diaSemana: new FormControl<DiaSemana | ''>(franja.diaSemana, { nonNullable: true }),
    horaInicio: new FormControl(franja.horaInicio, { nonNullable: true }),
    horaFin: new FormControl(franja.horaFin, { nonNullable: true }),
  });

/**
 * Formulario de grupo (`/admin/grupos/nuevo` y `/admin/grupos/:id`). No usa el formulario genérico
 * porque los horarios son una lista de filas que se agregan y se quitan.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-grupo-formulario',
  /** Dependencias de la plantilla. */
  imports: [ReactiveFormsModule, RouterLink, AlertaErroresComponent],
  /** Plantilla HTML. */
  templateUrl: './grupo-formulario.component.html',
})
export class GrupoFormularioComponent implements OnInit {
  /** Servicio de grupos. */
  private readonly grupos = inject(GrupoService);

  /** Servicio del plan de estudios. */
  private readonly asignaturasServicio = inject(AsignaturaService);

  /** Servicio de periodos. */
  private readonly periodosServicio = inject(PeriodoService);

  /** Servicio de usuarios, para la lista de profesores. */
  private readonly usuarios = inject(UsuarioService);

  /** Avisos. */
  private readonly avisos = inject(ToastService);

  /** Ruta actual. */
  private readonly ruta = inject(ActivatedRoute);

  /** Enrutador. */
  private readonly router = inject(Router);

  /** Días disponibles para las franjas. */
  protected readonly dias = DIAS;

  /** Formulario completo. */
  protected readonly formulario = new FormGroup({
    asignaturaId: new FormControl<number | null>(null, Validators.required),
    periodoId: new FormControl<number | null>(null, Validators.required),
    profesorId: new FormControl<number | null>(null, Validators.required),
    bloque: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(20)],
    }),
    salon: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(20)],
    }),
    horarios: new FormArray<FormGroup<ControlesFranja>>([crearFranja()]),
  });

  /** Asignaturas del plan, por semestre y código. */
  protected readonly asignaturas = signal<AsignaturaDto[]>([]);

  /** Periodos, del más reciente al más antiguo. */
  protected readonly periodos = signal<PeriodoDto[]>([]);

  /** Profesores, por nombre. */
  protected readonly profesores = signal<UsuarioDto[]>([]);

  /** Grupo que se modifica; `null` al crear. */
  protected readonly grupo = signal<GrupoDto | null>(null);

  /** Indica que se están cargando los datos. */
  protected readonly cargando = signal(true);

  /** Indica que se está guardando. */
  protected readonly enviando = signal(false);

  /** Mensajes de error generales. */
  protected readonly errores = signal<string[]>([]);

  /** Errores por campo devueltos por el backend. */
  protected readonly erroresCampo = signal<Record<string, string>>({});

  /** Con inscritos, asignatura, periodo y horarios no se pueden cambiar (el backend lo exige igual). */
  protected readonly bloqueado = computed(() => (this.grupo()?.inscritos ?? 0) > 0);

  /** Título de la pantalla. */
  protected readonly titulo = computed(() => {
    const actual = this.grupo();
    return actual
      ? `Modificar ${actual.asignatura.codigo} · grupo ${actual.numero} (${actual.periodo.codigo})`
      : 'Nuevo grupo';
  });

  /** Lista de franjas del formulario. */
  protected get franjas(): FormArray<FormGroup<ControlesFranja>> {
    return this.formulario.controls.horarios;
  }

  /** Carga las listas de los selectores y, si se modifica, el grupo. */
  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.paramMap.get('id')) || null;
    forkJoin({
      asignaturas: this.asignaturasServicio.listar(),
      periodos: this.periodosServicio.listar(),
      profesores: this.usuarios.listar('PROFESOR', '', 0, MAXIMO_PROFESORES),
      grupo: id === null ? of(null) : this.grupos.obtener(id),
    }).subscribe({
      next: (datos) => {
        this.asignaturas.set(
          [...datos.asignaturas].sort((a, b) => a.semestre - b.semestre || a.codigo.localeCompare(b.codigo)),
        );
        this.periodos.set([...datos.periodos].sort((a, b) => b.codigo.localeCompare(a.codigo)));
        this.profesores.set(datos.profesores.contenido);
        if (datos.grupo) {
          this.cargarGrupo(datos.grupo);
        } else {
          this.formulario.controls.periodoId.setValue(this.periodoSugerido(datos.periodos));
        }
        this.cargando.set(false);
      },
      error: (error: unknown) => {
        this.cargando.set(false);
        this.errores.set(mensajesDe(error));
      },
    });
  }

  /** Agrega una franja vacía. */
  protected agregarFranja(): void {
    this.franjas.push(crearFranja());
  }

  /**
   * Quita una franja.
   *
   * @param indice posición de la franja
   */
  protected quitarFranja(indice: number): void {
    this.franjas.removeAt(indice);
  }

  /**
   * Mensaje de error de un campo: primero el del servidor, luego el de la validación local.
   *
   * @param nombre nombre del campo
   * @return mensaje, o cadena vacía si está bien o aún no se tocó
   */
  protected error(nombre: 'asignaturaId' | 'periodoId' | 'profesorId' | 'bloque' | 'salon'): string {
    const delServidor = this.erroresCampo()[nombre];
    if (delServidor) {
      return delServidor;
    }
    const control = this.formulario.controls[nombre];
    if (!control.touched || !control.errors) {
      return '';
    }
    return control.errors['maxlength'] ? 'No puede tener más de 20 caracteres.' : 'Este campo es obligatorio.';
  }

  /** Valida y crea o modifica el grupo. */
  protected guardar(): void {
    this.formulario.markAllAsTouched();
    this.erroresCampo.set({});
    const franjas = this.franjas.getRawValue();
    const erroresHorario = validarHorarios(franjas);
    this.errores.set(erroresHorario);
    if (this.formulario.invalid || erroresHorario.length > 0) {
      return;
    }
    const valores = this.formulario.getRawValue();
    const datos: GrupoRequest = {
      asignaturaId: Number(valores.asignaturaId),
      periodoId: Number(valores.periodoId),
      profesorId: Number(valores.profesorId),
      bloque: valores.bloque.trim(),
      salon: valores.salon.trim(),
      horarios: franjas as HorarioDto[],
    };
    const actual = this.grupo();
    const peticion = actual ? this.grupos.modificar(actual.id, datos) : this.grupos.crear(datos);
    this.enviando.set(true);
    peticion.subscribe({
      next: (guardado) => {
        this.enviando.set(false);
        this.avisos.exito(
          `Grupo ${guardado.numero} de ${guardado.asignatura.codigo} ${actual ? 'modificado' : 'creado'}.`,
        );
        void this.router.navigate(['/admin/grupos'], {
          queryParams: { periodoId: guardado.periodo.id },
        });
      },
      error: (error: unknown) => {
        this.enviando.set(false);
        this.errores.set(mensajesDe(error));
        this.erroresCampo.set(erroresPorCampo(error));
      },
    });
  }

  /**
   * Llena el formulario con un grupo existente y bloquea lo que no se puede cambiar si tiene inscritos.
   *
   * @param grupo grupo a modificar
   */
  private cargarGrupo(grupo: GrupoDto): void {
    this.grupo.set(grupo);
    this.franjas.clear();
    for (const franja of ordenarHorarios(grupo.horarios)) {
      this.franjas.push(crearFranja(franja));
    }
    this.formulario.patchValue({
      asignaturaId: grupo.asignatura.id,
      periodoId: grupo.periodo.id,
      profesorId: grupo.profesor.id,
      bloque: grupo.bloque,
      salon: grupo.salon,
    });
    if (grupo.inscritos > 0) {
      this.formulario.controls.asignaturaId.disable();
      this.formulario.controls.periodoId.disable();
      this.franjas.disable();
    }
  }

  /**
   * Periodo que se propone al crear: el de la URL (viene del filtro del listado) o el activo.
   *
   * @param periodos periodos existentes
   * @return identificador sugerido, o `null`
   */
  private periodoSugerido(periodos: PeriodoDto[]): number | null {
    const pedido = Number(this.ruta.snapshot.queryParamMap.get('periodoId')) || null;
    return (periodos.find((p) => p.id === pedido) ?? periodos.find((p) => p.activo))?.id ?? null;
  }
}
