import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html'
})
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly mustChange = computed(() => this.auth.passwordMustChange());
  readonly user = this.auth.user;

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: matchPasswords });

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const raw = this.form.getRawValue();
    this.auth.changePassword({
      currentPassword: raw.currentPassword,
      newPassword: raw.newPassword
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => {
        this.successMessage.set('Password updated. You may now continue.');
        this.form.reset();
        setTimeout(() => void this.router.navigate(['/dashboard']), 1200);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not change password.')
    });
  }
}

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const newPw = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return newPw && confirm && newPw !== confirm ? { mismatch: true } : null;
}
