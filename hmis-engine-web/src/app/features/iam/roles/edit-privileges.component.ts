import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { RoleService } from './role.service';
import { Privilege, Role } from './role.types';

@Component({
  selector: 'app-edit-privileges',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './edit-privileges.component.html'
})
export class EditPrivilegesComponent implements OnInit {
  @Input({ required: true }) role!: Role;

  private readonly roleService = inject(RoleService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly privileges = signal<Privilege[]>([]);
  readonly selected = signal<string[]>([]);
  readonly query = signal('');
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.selected.set([...this.role.privileges]);
    this.roleService.listPrivileges()
      .pipe(finalize(() => this.loading.set(false)))
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

  save(): void {
    if (this.submitting()) return;
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.roleService.replacePrivileges(this.role.uid, this.selected())
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (updated: Role) => this.activeModal.close(updated),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update privileges.')
      });
  }
}
