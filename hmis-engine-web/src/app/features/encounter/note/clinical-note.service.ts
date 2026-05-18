import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { ClinicalNote, SaveClinicalNoteRequest } from './clinical-note.types';

@Injectable({ providedIn: 'root' })
export class ClinicalNoteService {
  private readonly http = inject(HttpClient);

  private base(consultationUid: string): string {
    return `${environment.apiUrl}/encounters/consultations/uid/${consultationUid}/clinical-note`;
  }

  /** Returns the note (200) or null when none has been recorded yet (204). */
  get(consultationUid: string): Observable<ClinicalNote | null> {
    return this.http.get<ClinicalNote | null>(this.base(consultationUid));
  }

  save(consultationUid: string, req: SaveClinicalNoteRequest): Observable<ClinicalNote> {
    return this.http.put<ClinicalNote>(this.base(consultationUid), req);
  }
}
