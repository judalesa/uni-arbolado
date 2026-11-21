import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../environments/environment';
import { AsignaturaService } from './asignatura.service';

/** Pruebas del servicio del plan de estudios. */
describe('AsignaturaService', () => {
  /** El plan se pide con un GET a `/api/asignaturas`. */
  it('trae el plan de estudios', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    let total = -1;

    TestBed.inject(AsignaturaService)
      .listar()
      .subscribe((lista) => (total = lista.length));
    backend.expectOne({ method: 'GET', url: `${environment.apiUrl}/api/asignaturas` }).flush([]);

    expect(total).toBe(0);
    backend.verify();
  });
});
