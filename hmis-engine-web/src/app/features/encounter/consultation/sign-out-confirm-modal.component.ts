import { CommonModule } from '@angular/common';
import { Component, Input, inject, signal } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

/**
 * Operator confirmation gate for consultation sign-out (Complete). The backend
 * already preserves PAID downstream orders and only voids UNPAID lab / radiology
 * / pharmacy orders — this modal makes that consequence explicit and requires the
 * operator to confirm patient identity before the irreversible action fires.
 */
@Component({
  selector: 'app-sign-out-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sign-out-confirm-modal.component.html'
})
export class SignOutConfirmModalComponent {
  @Input({ required: true }) patientName!: string | null;
  @Input({ required: true }) patientNo!: string | null;

  protected readonly activeModal = inject(NgbActiveModal);

  /** Identity-confirmation checkbox; the confirm button stays disabled until ticked. */
  readonly identityConfirmed = signal(false);

  toggleIdentity(checked: boolean): void { this.identityConfirmed.set(checked); }

  confirm(): void {
    if (!this.identityConfirmed()) return;
    this.activeModal.close(true);
  }
}
