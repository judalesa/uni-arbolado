import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Pagina } from '../model/pagina.model';
import { CrearProfesorRequest, UsuarioDto } from '../model/usuario.model';

/** Roles que el administrador puede listar. */
export type RolListable = 'ESTUDIANTE' | 'PROFESOR';

/** Segmento de la URL de cada rol listable. */
const RECURSO: Record<RolListable, string> = {
  ESTUDIANTE: 'estudiantes',
  PROFESOR: 'profesores',
};

/** Llamadas de usuarios: listados del administrador (`/api/admin/...`) y perfil propio (`/api/usuarios/me`). */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  /** Cliente HTTP. */
  private readonly http = inject(HttpClient);

  /** Base de las rutas de administración de usuarios. */
  private readonly base = `${environment.apiUrl}/api/admin`;

  /**
   * Lista una página de usuarios de un rol, ordenados por nombre.
   *
   * @param rol rol a listar
   * @param busqueda texto a buscar en nombre, documento o correo; vacío para no filtrar
   * @param pagina número de página desde 0
   * @param tamano elementos por página
   * @return la página pedida
   */
  listar(rol: RolListable, busqueda = '', pagina = 0, tamano = 20): Observable<Pagina<UsuarioDto>> {
    let params = new HttpParams().set('pagina', pagina).set('tamano', tamano);
    const texto = busqueda.trim();
    if (texto) {
      params = params.set('q', texto);
    }
    return this.http.get<Pagina<UsuarioDto>>(`${this.base}/${RECURSO[rol]}`, { params });
  }

  /**
   * Crea un profesor; la cuenta nace activa, sin verificación de correo.
   *
   * @param datos datos del profesor
   * @return el profesor creado
   */
  crearProfesor(datos: CrearProfesorRequest): Observable<UsuarioDto> {
    return this.http.post<UsuarioDto>(`${this.base}/profesores`, datos);
  }

  /**
   * Datos del usuario de la sesión, para cualquier rol.
   *
   * @return el usuario autenticado
   */
  yo(): Observable<UsuarioDto> {
    return this.http.get<UsuarioDto>(`${environment.apiUrl}/api/usuarios/me`);
  }
}
