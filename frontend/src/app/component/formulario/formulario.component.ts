import { Component, computed, input, output } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
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

/** Validaciones posibles de un campo; cada una devuelve `null` si el campo no la pide. */
const REGLAS: ((campo: CampoFormulario) => ValidatorFn | null)[] = [
  (campo) => (campo.requerido ? Validators.required : null),
  (campo) => (campo.tipo === 'email' ? Validators.email : null),
  (campo) => (campo.minimo === undefined ? null : Validators.minLength(campo.minimo)),
  (campo) => (campo.maximo === undefined ? null : Validators.maxLength(campo.maximo)),
  (campo) => (campo.patron ? Validators.pattern(campo.patron) : null),
  (campo) => (campo.valorMinimo === undefined ? null : Validators.min(campo.valorMinimo)),
  (campo) => (campo.valorMaximo === undefined ? null : Validators.max(campo.valorMaximo)),
];

/** Mensaje de cada error de validación de Angular, en el orden en que se revisan. */
const MENSAJES: { error: string; texto: (campo: CampoFormulario) => string }[] = [
  { error: 'required', texto: () => 'Este campo es obligatorio.' },
  { error: 'email', texto: () => 'Escribe un correo válido.' },
  { error: 'minlength', texto: (campo) => `Debe tener al menos ${campo.minimo} caracteres.` },
  { error: 'maxlength', texto: (campo) => `No puede tener más de ${campo.maximo} caracteres.` },
  { error: 'pattern', texto: (campo) => campo.mensajePatron ?? 'El formato no es válido.' },
  { error: 'min', texto: (campo) => `El valor mínimo es ${campo.valorMinimo}.` },
  { error: 'max', texto: (campo) => `El valor máximo es ${campo.valorMaximo}.` },
];

/**
 * Validadores de Angular que pide la definición de un campo.
 *
 * @param campo definición del campo
 * @return validadores del campo
 */
const validadoresDe = (campo: CampoFormulario): ValidatorFn[] =>
  REGLAS.map((regla) => regla(campo)).filter((v): v is ValidatorFn => v !== null);

/**
 * Mensaje del primer error de validación local de un campo.
 *
 * @param campo definición del campo
 * @param errores errores del control
 * @return mensaje para el usuario
 */
const mensajeLocal = (campo: CampoFormulario, errores: ValidationErrors): string =>
  MENSAJES.find((mensaje) => errores[mensaje.error])?.texto(campo) ?? 'El valor no es válido.';

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
    if (!control?.touched || !control.errors) {
      return '';
    }
    return mensajeLocal(campo, control.errors);
  }

  /**
   * Nota que va bajo el campo: el error si lo hay y, si no, la ayuda.
   *
   * @param campo definición del campo
   * @return texto y clase de la nota, o `null` si no hay nada que mostrar
   */
  protected pie(campo: CampoFormulario): { texto: string; clase: string } | null {
    const mensaje = this.error(campo);
    if (mensaje) {
      return { texto: mensaje, clase: 'invalid-feedback d-block' };
    }
    return campo.ayuda ? { texto: campo.ayuda, clase: 'form-text' } : null;
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
          validators: validadoresDe(campo),
        },
      );
    }
    return new FormGroup(controles);
  }
}
