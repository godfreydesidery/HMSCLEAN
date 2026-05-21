import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { StaffDirectoryService, StaffOption } from '../../../core/directory/staff-directory.service';
import { StoreStaffService } from './store-staff.service';
import { StoreStaff } from './store-staff.types';
import { Store } from './store.types';

/**
 * Store admin "Keepers" panel: lists the store keepers affiliated with a store
 * and lets an admin add (from STORE_PERSON-role users) or remove them. Mirrors
 * the legacy StorePerson multi-store selector, inverted to the store.
 */
@Component({
  selector: 'app-store-keepers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './store-keepers.component.html'
})
export class StoreKeepersComponent implements OnInit {
  @Input({ required: true }) store!: Store;

  private readonly service = inject(StoreStaffService);
  private readonly staffService = inject(StaffDirectoryService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly assigned = signal<StoreStaff[]>([]);
  readonly allKeepers = signal<StaffOption[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedUserUid = new FormControl('', { nonNullable: true });

  /** STORE_PERSON-role users not already affiliated with this store. */
  readonly assignable = computed(() => {
    const taken = new Set(this.assigned().map((a) => a.userUid));
    return this.allKeepers().filter((s) => !taken.has(s.uid));
  });

  ngOnInit(): void {
    this.staffService.byRole('STORE_PERSON').subscribe({
      next: (rows) => this.allKeepers.set(rows),
      error: () => this.errorMessage.set('Could not load store keeper directory.')
    });
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.service.list(this.store.uid)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.assigned.set(rows),
        error: () => this.errorMessage.set('Could not load assigned keepers.')
      });
  }

  assign(): void {
    const userUid = this.selectedUserUid.value;
    if (!userUid || this.busy()) { return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.assign(this.store.uid, userUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => { this.selectedUserUid.setValue(''); this.reload(); },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not assign keeper.')
      });
  }

  remove(row: StoreStaff): void {
    if (this.busy()) { return; }
    this.busy.set(true);
    this.errorMessage.set(null);
    this.service.remove(this.store.uid, row.userUid)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => this.reload(),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not remove keeper.')
      });
  }
}
