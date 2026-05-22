import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { OrderAttachmentService } from './attachment.service';
import { OrderAttachment } from './attachment.types';

@Component({
  selector: 'app-attachments-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './attachments-modal.component.html'
})
export class AttachmentsModalComponent implements OnInit {
  /** Order this modal manages attachments for. */
  @Input({ required: true }) orderUid!: string;
  /** Display tag for the header (order number or kind). */
  @Input() orderLabel: string | null = null;

  private readonly fb = inject(FormBuilder);
  private readonly attachmentService = inject(OrderAttachmentService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly attachments = signal<OrderAttachment[]>([]);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly downloadingUid = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedFile = signal<File | null>(null);

  readonly form = this.fb.nonNullable.group({
    description: ['']
  });

  ngOnInit(): void { this.load(); }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.attachmentService.listForOrder(this.orderUid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.attachments.set(rows),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load attachments.')
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.selectedFile.set(file);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file || this.uploading()) return;
    this.uploading.set(true);
    this.errorMessage.set(null);
    this.attachmentService.upload(this.orderUid, file, this.form.controls.description.value)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (saved) => {
          this.attachments.update((rows) => [...rows, saved]);
          this.selectedFile.set(null);
          this.form.reset({ description: '' });
          // Clear the file input element so the same file can be re-picked.
          const fileInput = document.getElementById('attachmentFile') as HTMLInputElement | null;
          if (fileInput) fileInput.value = '';
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Upload failed.')
      });
  }

  download(a: OrderAttachment): void {
    if (this.downloadingUid()) return;
    this.downloadingUid.set(a.uid);
    this.errorMessage.set(null);
    this.attachmentService.download(a.uid)
      .pipe(finalize(() => this.downloadingUid.set(null)))
      .subscribe({
        next: (blob) => triggerBrowserDownload(blob, a.filename),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not download attachment.')
      });
  }

  remove(a: OrderAttachment): void {
    if (!globalThis.confirm(`Delete ${a.filename}? This cannot be undone.`)) return;
    this.errorMessage.set(null);
    this.attachmentService.delete(a.uid).subscribe({
      next: () => this.attachments.update((rows) => rows.filter((r) => r.uid !== a.uid)),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not delete attachment.')
    });
  }

  humanSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  iconFor(contentType: string): string {
    if (contentType.startsWith('image/')) return 'bi-image';
    if (contentType === 'application/pdf') return 'bi-file-earmark-pdf';
    if (contentType.startsWith('text/')) return 'bi-file-earmark-text';
    return 'bi-file-earmark';
  }
}

function triggerBrowserDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  // Let the browser flush the click before we revoke the object url.
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
