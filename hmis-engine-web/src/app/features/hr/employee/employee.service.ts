import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../core/http/page.types';
import {
  CreateEmployeeRequest, Employee, EmployeeSearchParams, EmploymentStatus,
  TerminateEmployeeRequest, UpdateEmployeeRequest
} from './employee.types';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/hr/employees`;

  search(params: EmployeeSearchParams = {}): Observable<PageResponse<Employee>> {
    let p = new HttpParams();
    if (params.query) p = p.set('query', params.query);
    if (params.status) p = p.set('status', params.status);
    if (params.designation) p = p.set('designation', params.designation);
    if (params.department) p = p.set('department', params.department);
    if (params.page !== undefined) p = p.set('page', String(params.page));
    if (params.size !== undefined) p = p.set('size', String(params.size));
    if (params.sort) p = p.set('sort', params.sort);
    return this.http.get<PageResponse<Employee>>(this.base, { params: p });
  }

  findByUid(uid: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.base}/uid/${uid}`);
  }

  create(req: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.base, req);
  }

  update(uid: string, req: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.base}/uid/${uid}`, req);
  }

  setStatus(uid: string, status: Exclude<EmploymentStatus, 'TERMINATED'>): Observable<Employee> {
    return this.http.put<Employee>(`${this.base}/uid/${uid}/status`, { status });
  }

  terminate(uid: string, req: TerminateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(`${this.base}/uid/${uid}/terminate`, req);
  }
}
