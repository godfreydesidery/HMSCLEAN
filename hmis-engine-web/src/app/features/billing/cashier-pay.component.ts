import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { PatientSearchComponent } from '../../shared/patient-search/patient-search.component';
import { Patient } from '../patient/patient.types';
import { CollectPaymentComponent } from './collect-payment.component';
import { InvoiceService } from './invoice.service';
import {
  INVOICE_LINE_KINDS, InvoiceLineKind, LINE_COVERAGE_STATUSES, LineCoverageStatus,
  PayableLine, PayLinesResult
} from './invoice.types';

interface Till { kind: InvoiceLineKind; label: string; icon: string; count: number; }

/**
 * Patient-first cashier (legacy payment tills). The cashier finds a patient,
 * sees their outstanding service lines segmented into per-service tills
 * (Registration | Consultation | Lab | …), ticks the lines to pay, watches a
 * running "selected bills" total, and collects — settling those lines and
 * releasing each paid order on its own.
 */
@Component({
  selector: 'app-cashier-pay',
  standalone: true,
  imports: [CommonModule, PatientSearchComponent],
  templateUrl: './cashier-pay.component.html'
})
export class CashierPayComponent {
  private readonly invoiceService = inject(InvoiceService);
  private readonly modal = inject(NgbModal);

  readonly patient = signal<Patient | null>(null);
  readonly lines = signal<PayableLine[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly receipt = signal<PayLinesResult | null>(null);

  /** Active service till; 'ALL' shows every outstanding line. */
  readonly activeKind = signal<InvoiceLineKind | 'ALL'>('ALL');
  /** lineUids the cashier has ticked. */
  readonly checked = signal<Set<string>>(new Set());

  /** Per-service tills that actually have outstanding lines, with their counts. */
  readonly tills = computed<Till[]>(() => {
    const all = this.lines();
    return INVOICE_LINE_KINDS
      .map((k) => ({ kind: k.value, label: k.label, icon: k.icon, count: all.filter((l) => l.kind === k.value).length }))
      .filter((t) => t.count > 0);
  });

  readonly visibleLines = computed<PayableLine[]>(() => {
    const kind = this.activeKind();
    const all = this.lines();
    return kind === 'ALL' ? all : all.filter((l) => l.kind === kind);
  });

  readonly selectedLines = computed<PayableLine[]>(() => {
    const set = this.checked();
    return this.lines().filter((l) => set.has(l.lineUid));
  });

  readonly runningTotal = computed(() =>
    this.selectedLines().reduce((sum, l) => sum + l.outstanding, 0));

  readonly currency = computed(() => this.selectedLines()[0]?.currency ?? this.lines()[0]?.currency ?? 'TZS');

  /** True when the ticked lines span more than one currency — collection is blocked. */
  readonly mixedCurrency = computed(() => new Set(this.selectedLines().map((l) => l.currency)).size > 1);

  readonly allVisibleChecked = computed(() => {
    const vis = this.visibleLines();
    const set = this.checked();
    return vis.length > 0 && vis.every((l) => set.has(l.lineUid));
  });

  onPatientSelected(p: Patient | null): void {
    this.patient.set(p);
    this.checked.set(new Set());
    this.activeKind.set('ALL');
    this.receipt.set(null);
    this.lines.set([]);
    if (p) { this.reload(); }
  }

  reload(): void {
    const p = this.patient();
    if (!p) { return; }
    this.loading.set(true);
    this.errorMessage.set(null);
    this.invoiceService.payableLines(p.uid).subscribe({
      next: (rows) => { this.lines.set(rows); this.loading.set(false); },
      error: (err) => {
        this.lines.set([]);
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Could not load outstanding bills.');
      }
    });
  }

  setKind(kind: InvoiceLineKind | 'ALL'): void {
    this.activeKind.set(kind);
  }

  isChecked(lineUid: string): boolean {
    return this.checked().has(lineUid);
  }

  toggle(lineUid: string): void {
    const set = new Set(this.checked());
    if (set.has(lineUid)) { set.delete(lineUid); } else { set.add(lineUid); }
    this.checked.set(set);
  }

  toggleAllVisible(): void {
    const set = new Set(this.checked());
    const vis = this.visibleLines();
    if (this.allVisibleChecked()) {
      vis.forEach((l) => set.delete(l.lineUid));
    } else {
      vis.forEach((l) => set.add(l.lineUid));
    }
    this.checked.set(set);
  }

  collect(): void {
    const p = this.patient();
    const selected = this.selectedLines();
    if (!p || selected.length === 0 || this.mixedCurrency()) { return; }
    const ref = this.modal.open(CollectPaymentComponent, { size: 'lg', backdrop: 'static' });
    const inst = ref.componentInstance as CollectPaymentComponent;
    inst.patientUid = p.uid;
    inst.currency = this.currency();
    inst.lineUids = selected.map((l) => l.lineUid);
    inst.total = this.runningTotal();
    ref.result.then(
      (result: PayLinesResult) => { this.receipt.set(result); this.checked.set(new Set()); this.reload(); },
      () => { /* dismissed */ }
    );
  }

  kindMeta(kind: InvoiceLineKind): { label: string; icon: string } {
    return INVOICE_LINE_KINDS.find((k) => k.value === kind) ?? { label: kind, icon: 'bi-receipt' };
  }

  coverageBadge(status: LineCoverageStatus): string {
    return LINE_COVERAGE_STATUSES.find((s) => s.value === status)?.badgeClass ?? 'text-bg-secondary-subtle';
  }
}
