import { TestBed } from '@angular/core/testing';

import { loginDePrueba } from '../util/pruebas.util';
import { SesionService } from './sesion.service';

/** Pruebas del servicio de sesión. */
describe('SesionService', () => {
  /** Limpia el almacenamiento y el inyector entre pruebas. */
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
  });

  /** Tras iniciar, el usuario, el rol y el token quedan disponibles. */
  it('guarda la sesión al iniciar', () => {
    const sesion = TestBed.inject(SesionService);
    sesion.iniciar(loginDePrueba('PROFESOR'));

    expect(sesion.rol()).toBe('PROFESOR');
    expect(sesion.token()).toBe('token-de-prueba');
    expect(sesion.estaVigente()).toBe(true);
  });

  /** Una sesión guardada sobrevive a recargar la página dentro de la pestaña. */
  it('recupera la sesión guardada', () => {
    TestBed.inject(SesionService).iniciar(loginDePrueba('ADMINISTRADOR'));
    TestBed.resetTestingModule();

    expect(TestBed.inject(SesionService).rol()).toBe('ADMINISTRADOR');
  });

  /** Un token vencido se descarta y no se envía. */
  it('descarta la sesión vencida', () => {
    const sesion = TestBed.inject(SesionService);
    sesion.iniciar(loginDePrueba('ESTUDIANTE', -1));

    expect(sesion.token()).toBeNull();
    expect(sesion.usuario()).toBeNull();
  });

  /** Cerrar borra la sesión de memoria y del almacenamiento. */
  it('cierra la sesión', () => {
    const sesion = TestBed.inject(SesionService);
    sesion.iniciar(loginDePrueba());
    sesion.cerrar();

    expect(sesion.estaVigente()).toBe(false);
    expect(sessionStorage.getItem('arbolado.sesion')).toBeNull();
  });

  /** Un contenido dañado en el almacenamiento no rompe la aplicación. */
  it('ignora un almacenamiento con contenido inválido', () => {
    sessionStorage.setItem('arbolado.sesion', '{no es json');

    expect(TestBed.inject(SesionService).estaVigente()).toBe(false);
  });
});
