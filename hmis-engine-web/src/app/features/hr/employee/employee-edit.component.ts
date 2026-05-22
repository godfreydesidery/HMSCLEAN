import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeFormComponent } from './employee-form.component';
import { EmployeeService } from './employee.service';
import { Employee } from './employee.types';

@Component({
  selector: 'app-employee-edit',
  standalone: true,
  imports: [CommonModule, EmployeeFormComponent],
  template: `
    @if (loading()) {
      <div class="hmis-empty"><i class="bi bi-hourglass-split"></i><div class="small">Loading employee…</div></div>
    } @else if (errorMessage()) {
      <div class="alert alert-danger">{{ errorMessage() }}</div>
    } @else {
      @if (employee(); as e) {
        <app-employee-form [existing]="e"></app-employee-form>
      }
    }
  `
})
export class EmployeeEditComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly employeeService = inject(EmployeeService);

  readonly employee = signal<Employee | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing employee identifier.');
      return;
    }
    this.employeeService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (e) => this.employee.set(e),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load employee.')
      });
  }
}
