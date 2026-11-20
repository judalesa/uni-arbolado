import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ColumnaTabla, TablaComponent } from './tabla.component';

/** Fila de ejemplo para la tabla. */
interface Fila {
  /** Nombre mostrado. */
  nombre: string;
  /** Valor opcional. */
  nota: number | null;
}

/** Anfitrión que prueba la columna de acciones, que depende de una plantilla del padre. */
@Component({
  /** Etiqueta del anfitrión. */
  selector: 'app-anfitrion-tabla',
  /** Dependencias de la plantilla. */
  imports: [TablaComponent],
  /** Plantilla con una plantilla de acciones. */
  template: `
    <app-tabla [columnas]="columnas" [filas]="filas">
      <ng-template #acciones let-fila
        ><button class="accion">Ver {{ fila.nombre }}</button></ng-template
      >
    </app-tabla>
  `,
})
class AnfitrionTabla {
  /** Columnas de ejemplo. */
  columnas: ColumnaTabla<Fila>[] = [{ titulo: 'Nombre', clave: 'nombre' }];

  /** Filas de ejemplo. */
  filas: Fila[] = [{ nombre: 'Ana', nota: 4 }];
}

/** Pruebas de la tabla genérica. */
describe('TablaComponent', () => {
  /** Columnas de ejemplo: una por propiedad y otra calculada. */
  const columnas: ColumnaTabla<Fila>[] = [
    { titulo: 'Nombre', clave: 'nombre' },
    { titulo: 'Nota', valor: (fila) => fila.nota?.toFixed(1) },
  ];

  /**
   * Crea la tabla con los datos indicados.
   *
   * @param filas filas a mostrar
   * @param extra otras entradas del componente
   * @return el componente ya renderizado
   */
  async function crear(filas: Fila[], extra: Record<string, unknown> = {}) {
    const fixture = TestBed.createComponent(TablaComponent);
    fixture.componentRef.setInput('columnas', columnas);
    fixture.componentRef.setInput('filas', filas);
    for (const [clave, valor] of Object.entries(extra)) {
      fixture.componentRef.setInput(clave, valor);
    }
    await fixture.whenStable();
    return fixture;
  }

  /** Dibuja una fila por elemento y un guion en los valores vacíos. */
  it('muestra las filas y un guion en los valores vacíos', async () => {
    const fixture = await crear([
      { nombre: 'Ana', nota: 4 },
      { nombre: 'Luis', nota: null },
    ]);
    const celdas = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('tbody td'),
    ).map((td) => td.textContent?.trim());

    expect(celdas).toEqual(['Ana', '4.0', 'Luis', '—']);
  });

  /** Sin filas aparece el mensaje de vacío. */
  it('muestra el mensaje cuando no hay filas', async () => {
    const fixture = await crear([], { vacio: 'Sin grupos' });

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Sin grupos');
  });

  /** Mientras carga se muestra el estado de carga y no las filas. */
  it('muestra el estado de carga', async () => {
    const fixture = await crear([{ nombre: 'Ana', nota: 4 }], { cargando: true });
    const texto = (fixture.nativeElement as HTMLElement).textContent;

    expect(texto).toContain('Cargando');
    expect(texto).not.toContain('Ana');
  });

  /** El pie de paginación emite la página pedida y no sale del rango. */
  it('emite el cambio de página dentro del rango', async () => {
    const fixture = await crear([{ nombre: 'Ana', nota: 4 }], {
      paginacion: { pagina: 0, totalPaginas: 2, totalElementos: 30 },
    });
    const emitidas: number[] = [];
    fixture.componentInstance.cambioPagina.subscribe((pagina) => emitidas.push(pagina));
    const botones = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'nav button',
    );

    expect(botones[0].disabled).toBe(true);
    botones[1].click();
    expect(emitidas).toEqual([1]);
  });

  /** La plantilla de acciones del padre se dibuja en cada fila. */
  it('dibuja la columna de acciones', async () => {
    const fixture = TestBed.createComponent(AnfitrionTabla);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('.accion')?.textContent).toContain(
      'Ver Ana',
    );
  });
});
