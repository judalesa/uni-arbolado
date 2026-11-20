import { TestBed } from '@angular/core/testing';
import { PartialMatchRouteSnapshot, Route, Router, UrlSegment, UrlTree } from '@angular/router';

import { autenticadoGuard, rolGuard, visitanteGuard } from './acceso.guard';
import { loginDePrueba } from '../util/pruebas.util';
import { SesionService } from '../service/sesion.service';

/** Pruebas de los guards de rutas. */
describe('guards', () => {
  /** Sesión de la prueba. */
  let sesion: SesionService;

  /** Prepara una sesión vacía. */
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.resetTestingModule();
    sesion = TestBed.inject(SesionService);
  });

  /**
   * Ejecuta un guard dentro del inyector de la prueba.
   *
   * @param guard guard a ejecutar
   * @return lo que decide el guard
   */
  function ejecutar(guard: typeof autenticadoGuard): boolean | UrlTree {
    return TestBed.runInInjectionContext(() =>
      guard({} as Route, [] as UrlSegment[], {} as PartialMatchRouteSnapshot),
    ) as boolean | UrlTree;
  }

  /**
   * Convierte el resultado de un guard en la ruta a la que redirige.
   *
   * @param resultado lo que devolvió el guard
   * @return la URL de destino
   */
  function destino(resultado: boolean | UrlTree): string {
    return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
  }

  /** Sin sesión, una rama protegida lleva a `/login`. */
  it('rolGuard envía al login si no hay sesión', () => {
    expect(destino(ejecutar(rolGuard('ADMINISTRADOR')))).toBe('/login');
  });

  /** Con el rol correcto, deja pasar. */
  it('rolGuard permite el rol indicado', () => {
    sesion.iniciar(loginDePrueba('ADMINISTRADOR'));

    expect(ejecutar(rolGuard('ADMINISTRADOR'))).toBe(true);
  });

  /** Con otro rol, lleva a `/403`. */
  it('rolGuard envía a /403 si el rol no corresponde', () => {
    sesion.iniciar(loginDePrueba('ESTUDIANTE'));

    expect(destino(ejecutar(rolGuard('ADMINISTRADOR', 'PROFESOR')))).toBe('/403');
  });

  /** Un token vencido cuenta como no tener sesión. */
  it('rolGuard trata una sesión vencida como ausente', () => {
    sesion.iniciar(loginDePrueba('ADMINISTRADOR', -5));

    expect(destino(ejecutar(rolGuard('ADMINISTRADOR')))).toBe('/login');
  });

  /** Las pantallas para cualquier rol exigen solo sesión. */
  it('autenticadoGuard exige sesión', () => {
    expect(destino(ejecutar(autenticadoGuard))).toBe('/login');
    sesion.iniciar(loginDePrueba('PROFESOR'));
    expect(ejecutar(autenticadoGuard)).toBe(true);
  });

  /** Quien ya inició sesión no vuelve al login: va a su pantalla inicial. */
  it('visitanteGuard redirige a quien ya tiene sesión', () => {
    expect(ejecutar(visitanteGuard)).toBe(true);
    sesion.iniciar(loginDePrueba('PROFESOR'));
    expect(destino(ejecutar(visitanteGuard))).toBe('/profesor/grupos');
  });
});
