import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs';

import { EditPrivilegesComponent } from './edit-privileges.component';
import { RoleFormComponent } from './role-form.component';
import { RoleService } from './role.service';
import { Role } from './role.types';

@Component({
  selector: 'app-iam-roles',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './roles.component.html'
})
export class RolesComponent {
  private readonly roleService = inject(RoleService);
  private readonly modal = inject(NgbModal);

  readonly query = new FormControl('', { nonNullable: true });
  readonly roles = signal<Role[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);

  readonly filtered = computed(() => {
    const q = this.searchTerm().trim().toLowerCase();
    if (!q) return this.roles();
    return this.roles().filter((r) =>
      r.name.toLowerCase().includes(q)
      || (r.description ?? '').toLowerCase().includes(q)
      || r.privileges.some((p) => p.toLowerCase().includes(q)));
  });

  private readonly searchTerm = signal('');

  constructor() {
    this.refresh();
    this.query.valueChanges.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe((v) => this.searchTerm.set(v));
  }

  refresh(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.roleService.list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rs) => this.roles.set(rs),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load roles.')
      });
  }

  openCreate(): void {
    const ref = this.modal.open(RoleFormComponent, { size: 'lg', backdrop: 'static' });
    ref.closed.subscribe((r: Role | undefined) => {
      if (r) { this.actionMessage.set(`Role "${r.name}" created.`); this.refresh(); }
    });
  }

  openEditPrivileges(r: Role): void {
    const ref = this.modal.open(EditPrivilegesComponent, { size: 'lg', backdrop: 'static' });
    (ref.componentInstance as EditPrivilegesComponent).role = r;
    ref.closed.subscribe((updated: Role | undefined) => {
      if (updated) { this.actionMessage.set('Privileges updated.'); this.refresh(); }
    });
  }
}
