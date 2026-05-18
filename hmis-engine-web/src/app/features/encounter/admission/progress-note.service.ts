import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreateProgressNoteRequest, ProgressNote } from './progress-note.types';

@Injectable({ providedIn: 'root' })
export class ProgressNoteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters`;

  list(admissionUid: string): Observable<ProgressNote[]> {
    return this.http.get<ProgressNote[]>(`${this.base}/admissions/uid/${admissionUid}/progress-notes`);
  }

  add(admissionUid: string, req: CreateProgressNoteRequest): Observable<ProgressNote> {
    return this.http.post<ProgressNote>(`${this.base}/admissions/uid/${admissionUid}/progress-notes`, req);
  }

  softDelete(uid: string, reason: string | null): Observable<ProgressNote> {
    return this.http.request<ProgressNote>('DELETE', `${this.base}/progress-notes/uid/${uid}`, { body: { reason } });
  }
}
