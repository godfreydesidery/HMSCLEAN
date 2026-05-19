import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { OrderAttachment } from './attachment.types';

@Injectable({ providedIn: 'root' })
export class OrderAttachmentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/encounters`;

  listForOrder(orderUid: string): Observable<OrderAttachment[]> {
    return this.http.get<OrderAttachment[]>(`${this.base}/orders/uid/${orderUid}/attachments`);
  }

  /** Multipart upload. Backend accepts `file` (required) + `description` (optional). */
  upload(orderUid: string, file: File, description?: string | null): Observable<OrderAttachment> {
    const form = new FormData();
    form.append('file', file, file.name);
    if (description && description.trim()) form.append('description', description.trim());
    return this.http.post<OrderAttachment>(
      `${this.base}/orders/uid/${orderUid}/attachments`,
      form
    );
  }

  /** Stream bytes back as a Blob so the browser can offer download / preview. */
  download(attachmentUid: string): Observable<Blob> {
    return this.http.get(`${this.base}/attachments/uid/${attachmentUid}/download`, {
      responseType: 'blob'
    });
  }

  delete(attachmentUid: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/attachments/uid/${attachmentUid}`);
  }
}
