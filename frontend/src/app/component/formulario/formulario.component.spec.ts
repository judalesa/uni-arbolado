import { TestBed } from '@angular/core/testing';

import { CampoFormulario, FormularioComponent, ValoresFormulario } from './formulario.component';

/** Pruebas del formulario genérico. */
describe('FormularioComponent', () => {
  /** Campos de ejemplo. */
  const campos: CampoFormulario[] = [
    { nombre: 'correo', etiqueta: 'Correo', tipo: 'email', requerido: true },
    {
      nombre: 'documento',
      etiqueta: 'Documento',
      tipo: 'text',
      requerido: true,
      patron: '^[0-9]{10}$',
      mensajePatron: 'Deben ser 10 dígitos',
    },
  ];

  /**
   * Crea el formulario con los campos de ejemplo.
   *
   * @param extra otras entradas del componente
   * @return el componente y los valores emitidos
   */
  async function crear(extra: Record<string, unknown> = {}) {
    const fixture = TestBed.createComponent(FormularioComponent);
    fixture.componentRef.setInput('campos', campos);
    for (const [clave, valor] of Object.entries(extra)) {
      fixture.componentRef.setInput(clave, valor);
    }
    const emitidos: ValoresFormulario[] = [];
    fixture.componentInstance.enviar.subscribe((valores) => emitidos.push(valores));
    await fixture.whenStable();
    return { fixture, emitidos, html: fixture.nativeElement as HTMLElement };
  }

  /** Con campos inválidos no emite y muestra los errores. */
  it('no emite si hay campos inválidos y muestra los errores', async () => {
    const { fixture, emitidos, html } = await crear();
    html.querySelector('form')?.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(emitidos).toHaveLength(0);
    expect(html.textContent).toContain('Este campo es obligatorio.');
  });

  /** Con todo válido emite los valores. */
  it('emite los valores cuando es válido', async () => {
    const { fixture, emitidos, html } = await crear({
      valores: { correo: 'ana@example.com', documento: '1012345678' },
    });
    html.querySelector('form')?.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(emitidos).toEqual([{ correo: 'ana@example.com', documento: '1012345678' }]);
  });

  /** Valida el patrón y muestra el mensaje propio del campo. */
  it('muestra el mensaje del patrón', async () => {
    const { fixture, html } = await crear({
      valores: { correo: 'ana@example.com', documento: '123' },
    });
    html.querySelector('form')?.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(html.textContent).toContain('Deben ser 10 dígitos');
  });

  /** Los errores que devuelve el backend aparecen junto a su campo. */
  it('muestra los errores del servidor por campo', async () => {
    const { html } = await crear({ erroresServidor: { correo: 'El correo ya está registrado' } });

    expect(html.textContent).toContain('El correo ya está registrado');
  });
});
