import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { RoleService } from './role.service';
import { Privilege, Role } from './role.types';

@Component({
  selector: 'app-role-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './role-form.component.html'
})
export class RoleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly roleService = inject(RoleService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly privileges = signal<Privilege[]>([]);
  readonly selected = signal<string[]>([]);
  readonly query = signal('');
  readonly loadingPrivs = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(64)]],
    description: ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.roleService.listPrivileges()
      .pipe(finalize(() => this.loadingPrivs.set(false)))
      .subscribe({
        next: (ps) => this.privileges.set(ps),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load privileges.')
      });
  }

  toggle(name: string, checked: boolean): void {
    if (checked) {
      this.selected.update((arr) => arr.includes(name) ? arr : [...arr, name]);
    } else {
      this.selected.update((arr) => arr.filter((n) => n !== name));
    }
  }
  isSelected(name: string): boolean { return this.selected().includes(name); }

  setQuery(v: string): void { this.query.set(v); }

  filteredPrivileges(): Privilege[] {
    const q = this.query().trim().toLowerCase();
    if (!q) return this.privileges();
    return this.privileges().filter((p) =>
      p.name.toLowerCase().includes(q) || (p.description ?? '').toLowerCase().includes(q));
  }

  submit(): void {
    if (this.submitting()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.roleService.create({
      name: raw.name.trim(),
      description: raw.description?.trim() || null,
      privileges: this.selected()
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (r: Role) => this.activeModal.close(r),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create role.')
    });
  }
}
