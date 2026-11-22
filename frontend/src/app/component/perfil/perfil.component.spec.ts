import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { environment } from '../../../environments/environment';
import { loginDePrueba } from '../../util/pruebas.util';
import { PerfilComponent } from './perfil.component';

/** Pruebas del perfil. */
describe('PerfilComponent', () => {
  /** Muestra los datos de la cuenta leídos del backend, sin opciones de edición. */
  it('muestra los datos de la cuenta', async () => {
    TestBed.configureTestingModule({
      imports: [PerfilComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PerfilComponent);
    const html = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne(`${environment.apiUrl}/api/usuarios/me`).flush(loginDePrueba('PROFESOR').usuario);
    await fixture.whenStable();

    expect(html.textContent).toContain('Ana Pérez');
    expect(html.textContent).toContain('1012345678');
    expect(html.textContent).toContain('Profesor');
    expect(html.textContent).toContain('Activo');
    expect(html.querySelector('button, input')).toBeNull();
    backend.verify();
  });
});
