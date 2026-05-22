import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { EmployeeService } from './employee.service';
import { EMPLOYMENT_STATUSES, Employee, EmploymentStatus } from './employee.types';
import { TerminateEmployeeModalComponent } from './terminate-employee-modal.component';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './employee-detail.component.html'
})
export class EmployeeDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);
  private readonly modal = inject(NgbModal);

  readonly statuses = EMPLOYMENT_STATUSES;

  readonly employee = signal<Employee | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly isTerminated = computed(() => this.employee()?.employmentStatus === 'TERMINATED');
  readonly canEdit      = computed(() => !this.isTerminated());
  readonly canTerminate = computed(() => !this.isTerminated() && !!this.employee());

  /** Status options the /status endpoint accepts (TERMINATED is via /terminate). */
  readonly settableStatuses = this.statuses.filter((s) => s.value !== 'TERMINATED');

  constructor() {
    const uid = this.route.snapshot.paramMap.get('uid');
    if (!uid) {
      this.loading.set(false);
      this.errorMessage.set('Missing employee identifier.');
      return;
    }
    this.load(uid);
  }

  private load(uid: string): void {
    this.loading.set(true);
    this.employeeService.findByUid(uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (e) => this.employee.set(e),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load employee.')
      });
  }

  setStatus(target: Exclude<EmploymentStatus, 'TERMINATED'>): void {
    const current = this.employee();
    if (!current || this.busy() || current.employmentStatus === target) return;
    this.busy.set(true);
    this.errorMessage.set(null);
    this.employeeService.setStatus(current.uid, target)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (e) => this.employee.set(e),
        error: (err) => this.errorMessage.set(err?.error?.message ?? `Could not set status to ${target}.`)
      });
  }

  openTerminate(): void {
    const current = this.employee();
    if (!current) return;
    const ref = this.modal.open(TerminateEmployeeModalComponent, { backdrop: 'static' });
    const inst = ref.componentInstance as TerminateEmployeeModalComponent;
    inst.employeeUid = current.uid;
    inst.employeeLabel = `${current.fullName} · ${current.employeeNo}`;
    ref.closed.subscribe((updated: Employee | undefined) => {
      if (updated) this.employee.set(updated);
    });
  }

  edit(): void {
    const current = this.employee();
    if (current) void this.router.navigate(['/hr/employees', current.uid, 'edit']);
  }

  statusBadgeClass(s: EmploymentStatus): string {
    return 'badge ' + (this.statuses.find((x) => x.value === s)?.badgeClass ?? '');
  }
  statusLabel(s: EmploymentStatus): string {
    return this.statuses.find((x) => x.value === s)?.label ?? s;
  }
}
