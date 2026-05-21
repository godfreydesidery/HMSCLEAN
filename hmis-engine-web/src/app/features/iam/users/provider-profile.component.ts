import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { ProviderProfileService } from './provider-profile.service';
import { User } from './user.types';

/**
 * Edit a clinician's provider profile (specialty / registration / licence).
 * Upsert semantics — the sidecar is created on first save.
 */
@Component({
  selector: 'app-provider-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './provider-profile.component.html'
})
export class ProviderProfileComponent implements OnInit {
  @Input({ required: true }) user!: User;

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ProviderProfileService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    specialty: ['', [Validators.maxLength(64)]],
    registrationNo: ['', [Validators.maxLength(64)]],
    licenseNo: ['', [Validators.maxLength(64)]]
  });

  ngOnInit(): void {
    this.service.get(this.user.uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (p) => this.form.patchValue({
          specialty: p.specialty ?? '',
          registrationNo: p.registrationNo ?? '',
          licenseNo: p.licenseNo ?? ''
        }),
        // 404 simply means no profile yet — start with a blank form.
        error: () => undefined
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.service.upsert(this.user.uid, {
      specialty: raw.specialty.trim() || null,
      registrationNo: raw.registrationNo.trim() || null,
      licenseNo: raw.licenseNo.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => this.activeModal.close(true),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not save provider profile.')
    });
  }
}
