import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CreateAnalyteRequest, CreateRangeRequest, LabReferenceRange, LabTestAnalyte,
  UpdateAnalyteRequest, UpdateRangeRequest
} from './lab-analyte.types';

@Injectable({ providedIn: 'root' })
export class LabAnalyteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/masterdata/lab-tests`;

  listAnalytes(labTestUid: string): Observable<LabTestAnalyte[]> {
    return this.http.get<LabTestAnalyte[]>(`${this.base}/uid/${labTestUid}/analytes`);
  }
  createAnalyte(labTestUid: string, req: CreateAnalyteRequest): Observable<LabTestAnalyte> {
    return this.http.post<LabTestAnalyte>(`${this.base}/uid/${labTestUid}/analytes`, req);
  }
  updateAnalyte(analyteUid: string, req: UpdateAnalyteRequest): Observable<LabTestAnalyte> {
    return this.http.put<LabTestAnalyte>(`${this.base}/analytes/uid/${analyteUid}`, req);
  }
  deleteAnalyte(analyteUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/analytes/uid/${analyteUid}`);
  }

  addRange(analyteUid: string, req: CreateRangeRequest): Observable<LabReferenceRange> {
    return this.http.post<LabReferenceRange>(`${this.base}/analytes/uid/${analyteUid}/ranges`, req);
  }
  updateRange(rangeUid: string, req: UpdateRangeRequest): Observable<LabReferenceRange> {
    return this.http.put<LabReferenceRange>(`${this.base}/analyte-ranges/uid/${rangeUid}`, req);
  }
  deleteRange(rangeUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/analyte-ranges/uid/${rangeUid}`);
  }
}
