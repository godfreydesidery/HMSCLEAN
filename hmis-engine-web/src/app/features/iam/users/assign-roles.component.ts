import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { UserService } from './user.service';
import { Role, User } from './user.types';

@Component({
  selector: 'app-assign-roles',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './assign-roles.component.html'
})
export class AssignRolesComponent implements OnInit {
  @Input({ required: true }) user!: User;

  private readonly userService = inject(UserService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly roles = signal<Role[]>([]);
  readonly selected = signal<string[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.selected.set([...this.user.roles]);
    this.userService.listRoles()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rs) => this.roles.set(rs),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load roles.')
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

  save(): void {
    if (this.submitting()) return;
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.userService.replaceRoles(this.user.uid, this.selected())
      .pipe(finalize(() => this.submitting.set(false))).subscribe({
        next: (updated: User) => this.activeModal.close(updated),
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not update roles.')
      });
  }
}
