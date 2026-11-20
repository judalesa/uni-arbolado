import { TestBed } from '@angular/core/testing';

import { ConfirmacionComponent } from './confirmacion.component';

/** Pruebas del cuadro de confirmación. */
describe('ConfirmacionComponent', () => {
  /** Cerrado no dibuja nada. */
  it('no se dibuja si está cerrado', async () => {
    const fixture = TestBed.createComponent(ConfirmacionComponent);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.modal')).toBeNull();
  });

  /** Abierto muestra el mensaje y emite según el botón. */
  it('emite confirmar y cancelar', async () => {
    const fixture = TestBed.createComponent(ConfirmacionComponent);
    fixture.componentRef.setInput('abierto', true);
    fixture.componentRef.setInput('mensaje', 'No podrás cancelarla');
    const eventos: string[] = [];
    fixture.componentInstance.confirmar.subscribe(() => eventos.push('confirmar'));
    fixture.componentInstance.cancelar.subscribe(() => eventos.push('cancelar'));
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    const botones = html.querySelectorAll<HTMLButtonElement>('.modal-footer button');

    expect(html.textContent).toContain('No podrás cancelarla');
    botones[0].click();
    botones[1].click();
    expect(eventos).toEqual(['cancelar', 'confirmar']);
  });
});
