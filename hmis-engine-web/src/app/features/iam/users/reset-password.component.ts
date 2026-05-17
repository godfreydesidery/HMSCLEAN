import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { UserService } from './user.service';
import { User } from './user.types';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent {
  @Input({ required: true }) user!: User;

  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  /** When set, holds the temp password just issued; UI switches to "share" mode. */
  readonly issuedPassword = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]]
  });

  /** Generates an 12-char temporary password. Letters + digits, easy to read aloud. */
  generate(): void {
    const alphabet = 'ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
    let out = '';
    const arr = new Uint32Array(12);
    crypto.getRandomValues(arr);
    for (let i = 0; i < 12; i++) {
      out += alphabet[arr[i] % alphabet.length];
    }
    this.form.controls.newPassword.setValue(out);
  }

  copy(): void {
    const pw = this.issuedPassword();
    if (pw) void navigator.clipboard.writeText(pw);
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const pw = this.form.controls.newPassword.value;
    this.userService.resetPassword(this.user.uid, pw)
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: () => this.issuedPassword.set(pw),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not reset password.')
      });
  }

  done(): void {
    // Close with truthy value so the caller knows to refresh.
    this.activeModal.close(true);
  }
}
