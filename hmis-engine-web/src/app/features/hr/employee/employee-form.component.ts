import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeService } from './employee.service';
import { CreateEmployeeRequest, Employee } from './employee.types';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './employee-form.component.html'
})
export class EmployeeFormComponent implements OnInit {
  /** Pass when editing — undefined for create. */
  readonly existing = input<Employee | null>(null);

  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    firstName:   ['', [Validators.required, Validators.maxLength(80)]],
    middleName:  ['', [Validators.maxLength(80)]],
    lastName:    ['', [Validators.required, Validators.maxLength(80)]],
    gender:      ['', [Validators.maxLength(16)]],
    dateOfBirth: [''],
    nationalId:  ['', [Validators.maxLength(32)]],
    phone:       ['', [Validators.maxLength(32)]],
    email:       ['', [Validators.maxLength(120)]],
    address:     ['', [Validators.maxLength(255)]],
    username:    ['', [Validators.maxLength(64)]],
    designation: ['', [Validators.maxLength(120)]],
    department:  ['', [Validators.maxLength(120)]],
    hireDate:    ['', [Validators.required]]
  });

  readonly isEdit = computed(() => !!this.existing());
  get title(): string { return this.isEdit() ? 'Edit employee' : 'Register employee'; }

  ngOnInit(): void {
    const e = this.existing();
    if (e) {
      this.form.controls.hireDate.disable();
      this.form.patchValue({
        firstName:   e.firstName,
        middleName:  e.middleName ?? '',
        lastName:    e.lastName,
        gender:      e.gender ?? '',
        dateOfBirth: e.dateOfBirth ?? '',
        nationalId:  e.nationalId ?? '',
        phone:       e.phone ?? '',
        email:       e.email ?? '',
        address:     e.address ?? '',
        username:    e.username ?? '',
        designation: e.designation ?? '',
        department:  e.department ?? '',
        hireDate:    e.hireDate
      });
    }
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    const raw = this.form.getRawValue();
    const payload: CreateEmployeeRequest = {
      firstName:   raw.firstName.trim(),
      middleName:  emptyToNull(raw.middleName),
      lastName:    raw.lastName.trim(),
      gender:      emptyToNull(raw.gender),
      dateOfBirth: emptyToNull(raw.dateOfBirth),
      nationalId:  emptyToNull(raw.nationalId),
      phone:       emptyToNull(raw.phone),
      email:       emptyToNull(raw.email),
      address:     emptyToNull(raw.address),
      username:    emptyToNull(raw.username),
      designation: emptyToNull(raw.designation),
      department:  emptyToNull(raw.department),
      hireDate:    raw.hireDate
    };

    const existing = this.existing();
    const { hireDate: _droppedHireDate, ...updatePayload } = payload;
    void _droppedHireDate;
    const req$ = existing
      ? this.employeeService.update(existing.uid, updatePayload)
      : this.employeeService.create(payload);

    req$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (saved) => void this.router.navigate(['/hr/employees', saved.uid]),
      error: (err) => {
        const fieldErrors: { field: string; message: string }[] = err?.error?.errors ?? [];
        const summary = fieldErrors.map((fe) => `${fe.field}: ${fe.message}`).join('; ');
        this.errorMessage.set(summary || err?.error?.message || 'Could not save employee.');
      }
    });
  }

  cancel(): void {
    void this.router.navigate(['/hr/employees']);
  }
}

function emptyToNull(v: string | null | undefined): string | null {
  if (v == null) return null;
  const t = v.trim();
  return t === '' ? null : t;
}
