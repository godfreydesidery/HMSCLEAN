import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { WorkingLocationService } from '../../core/working-location/working-location.service';

/**
 * The store WORKSPACE — the persistent shell for every store operation. Once a store is
 * selected (via the {@code select} child / picker), a compact action toolbar stays ON
 * SCREEN, and the chosen operation renders in the nested {@code <router-outlet>}. Mirrors
 * legacy: the action toolbar is always visible so switching Stock ↔ Transfers is a single
 * click — no side-menu round-trip. When no store is selected the toolbar is hidden and the
 * outlet shows the picker.
 *
 * The Transfers menu is a plain Angular-controlled dropdown (a signal + (click) items),
 * not the ng-bootstrap dropdown — it must navigate reliably from inside the nav-pills bar.
 */
@Component({
  selector: 'app-store-workspace',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './store-workspace.component.html'
})
export class StoreWorkspaceComponent {
  private readonly workingLocation = inject(WorkingLocationService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly workingStore = this.workingLocation.workingStore;
  readonly transfersOpen = signal(false);

  toggleTransfers(): void { this.transfersOpen.update((v) => !v); }

  /** Navigate to a transfers screen and close the menu. */
  go(path: string): void {
    this.transfersOpen.set(false);
    void this.router.navigate([path]);
  }

  /** Close the Transfers menu when clicking anywhere outside the workspace bar. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.transfersOpen() && !this.host.nativeElement.contains(event.target)) {
      this.transfersOpen.set(false);
    }
  }
}
