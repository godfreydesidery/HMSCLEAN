import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { UserService } from './user.service';
import { Role, User } from './user.types';

@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-form.component.html'
})
export class UserFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly roles = signal<Role[]>([]);
  readonly loadingRoles = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(64)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    roles: this.fb.nonNullable.array<string>([])
  });

  ngOnInit(): void {
    this.loadingRoles.set(true);
    this.userService.listRoles()
      .pipe(finalize(() => this.loadingRoles.set(false)))
      .subscribe({ next: (rs) => this.roles.set(rs) });
  }

  toggleRole(name: string, checked: boolean): void {
    const arr = this.form.controls.roles;
    const current = arr.getRawValue();
    if (checked && !current.includes(name)) {
      arr.push(this.fb.nonNullable.control(name));
    } else if (!checked) {
      const idx = current.indexOf(name);
      if (idx >= 0) arr.removeAt(idx);
    }
  }

  hasRole(name: string): boolean {
    return this.form.controls.roles.getRawValue().includes(name);
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.userService.create({
      username: raw.username.trim(),
      password: raw.password,
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      email: raw.email?.trim() || null,
      roles: raw.roles
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (u: User) => this.activeModal.close(u),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create user.')
    });
  }
}
