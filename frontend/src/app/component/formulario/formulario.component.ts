import { Component, computed, input, output } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';

/** Tipos de campo que sabe dibujar el formulario genérico. */
export type TipoCampo = 'text' | 'email' | 'password' | 'number' | 'date' | 'select' | 'textarea';

/** Opción de un campo `select`. */
export interface OpcionCampo {
  /** Valor que se envía. */
  valor: string | number;
  /** Texto visible. */
  etiqueta: string;
}

/** Definición de un campo del formulario. */
export interface CampoFormulario {
  /** Nombre del campo; coincide con la propiedad del DTO y con `detalles[].campo` del backend. */
  nombre: string;
  /** Etiqueta visible. */
  etiqueta: string;
  /** Tipo de entrada. */
  tipo: TipoCampo;
  /** Si es obligatorio. */
  requerido?: boolean;
  /** Longitud mínima de texto. */
  minimo?: number;
  /** Longitud máxima de texto. */
  maximo?: number;
  /** Valor numérico mínimo (solo `number`). */
  valorMinimo?: number;
  /** Valor numérico máximo (solo `number`). */
  valorMaximo?: number;
  /** Paso numérico (solo `number`). */
  paso?: number;
  /** Expresión regular que debe cumplir el texto. */
  patron?: string;
  /** Mensaje cuando no cumple el patrón. */
  mensajePatron?: string;
  /** Opciones (solo `select`). */
  opciones?: OpcionCampo[];
  /** Texto de ayuda bajo el campo. */
  ayuda?: string;
  /** Texto de ejemplo dentro del campo. */
  marcador?: string;
  /** Valor `autocomplete` del navegador. */
  autocompletar?: string;
}

/** Valores del formulario, por nombre de campo. */
export type ValoresFormulario = Record<string, string | number | null>;

/**
 * Formulario genérico: se declara con una lista de campos y se encarga de validar, mostrar los errores
 * (los locales y los que devuelve el backend por campo) y emitir los valores. Evita repetir el mismo
 * formulario en cada pantalla de registro, creación o edición.
 */
@Component({
  /** Etiqueta del componente. */
  selector: 'app-formulario',
  /** Dependencias de la plantilla. */
  imports: [ReactiveFormsModule],
  /** Plantilla HTML. */
  templateUrl: './formulario.component.html',
})
export class FormularioComponent {
  /** Campos a dibujar. */
  readonly campos = input.required<CampoFormulario[]>();

  /** Valores iniciales, por nombre de campo (por ejemplo al editar). */
  readonly valores = input<ValoresFormulario>({});

  /** Texto del botón de envío. */
  readonly etiquetaEnviar = input('Guardar');

  /** Indica que se está enviando; deshabilita el botón. */
  readonly enviando = input(false);

  /** Errores devueltos por el backend, por nombre de campo. */
  readonly erroresServidor = input<Record<string, string>>({});

  /** Emite los valores cuando el formulario es válido. */
  readonly enviar = output<ValoresFormulario>();

  /** Grupo de controles; se rehace si cambian los campos o los valores iniciales. */
  protected readonly grupo = computed(() => this.crearGrupo(this.campos(), this.valores()));

  /** Valida y, si todo está bien, emite los valores. */
  protected alEnviar(): void {
    const grupo = this.grupo();
    grupo.markAllAsTouched();
    if (grupo.valid) {
      this.enviar.emit(grupo.getRawValue());
    }
  }

  /**
   * Mensaje de error de un campo: primero el del servidor, luego el de la validación local.
   *
   * @param campo definición del campo
   * @return mensaje, o cadena vacía si el campo está bien o aún no se tocó
   */
  protected error(campo: CampoFormulario): string {
    const delServidor = this.erroresServidor()[campo.nombre];
    if (delServidor) {
      return delServidor;
    }
    const control = this.grupo().controls[campo.nombre];
    if (!control || !control.touched || !control.errors) {
      return '';
    }
    const errores = control.errors;
    if (errores['required']) {
      return 'Este campo es obligatorio.';
    }
    if (errores['email']) {
      return 'Escribe un correo válido.';
    }
    if (errores['minlength']) {
      return `Debe tener al menos ${campo.minimo} caracteres.`;
    }
    if (errores['maxlength']) {
      return `No puede tener más de ${campo.maximo} caracteres.`;
    }
    if (errores['pattern']) {
      return campo.mensajePatron ?? 'El formato no es válido.';
    }
    if (errores['min']) {
      return `El valor mínimo es ${campo.valorMinimo}.`;
    }
    if (errores['max']) {
      return `El valor máximo es ${campo.valorMaximo}.`;
    }
    return 'El valor no es válido.';
  }

  /**
   * Construye el grupo de controles con sus validaciones.
   *
   * @param campos definiciones de los campos
   * @param valores valores iniciales
   * @return grupo listo para enlazar a la plantilla
   */
  private crearGrupo(
    campos: CampoFormulario[],
    valores: ValoresFormulario,
  ): FormGroup<Record<string, FormControl<string | number | null>>> {
    const controles: Record<string, FormControl<string | number | null>> = {};
    for (const campo of campos) {
      const vacio = campo.tipo === 'number' ? null : '';
      controles[campo.nombre] = new FormControl<string | number | null>(
        valores[campo.nombre] ?? vacio,
        {
          validators: this.validadores(campo),
        },
      );
    }
    return new FormGroup(controles);
  }

  /**
   * Arma los validadores de un campo a partir de su definición.
   *
   * @param campo definición del campo
   * @return validadores de Angular
   */
  private validadores(campo: CampoFormulario): ValidatorFn[] {
    const lista: ValidatorFn[] = [];
    if (campo.requerido) {
      lista.push(Validators.required);
    }
    if (campo.tipo === 'email') {
      lista.push(Validators.email);
    }
    if (campo.minimo !== undefined) {
      lista.push(Validators.minLength(campo.minimo));
    }
    if (campo.maximo !== undefined) {
      lista.push(Validators.maxLength(campo.maximo));
    }
    if (campo.patron) {
      lista.push(Validators.pattern(campo.patron));
    }
    if (campo.valorMinimo !== undefined) {
      lista.push(Validators.min(campo.valorMinimo));
    }
    if (campo.valorMaximo !== undefined) {
      lista.push(Validators.max(campo.valorMaximo));
    }
    return lista;
  }
}
