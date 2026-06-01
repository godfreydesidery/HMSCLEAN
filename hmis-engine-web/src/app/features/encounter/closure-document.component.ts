import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { PrintLayoutComponent, PrintMeta } from '../../shared/print/print-layout.component';

export type ClosureDocumentKind = 'DISCHARGE' | 'DECEASED' | 'REFERRAL';

/**
 * The data a closure document renders — a flattened view of the closure plan
 * (DischargePlanDto) plus the patient/encounter display fields the plan DTO does
 * not itself carry. Both callers (admission discharge plan + consultation
 * closure) map their own plan type into this shape, so the document stays
 * decoupled from either.
 */
export interface ClosureDocumentData {
  kind: ClosureDocumentKind;
  status: string;
  patientName: string | null;
  patientNo: string | null;
  encounterLabel: string;        // 'Admission' | 'Consultation'
  encounterNo: string | null;
  history: string | null;
  investigation: string | null;
  management: string | null;
  operationNote: string | null;
  icuNote: string | null;
  recommendations: string | null;
  referralFacility: string | null;
  /** Registered external-provider name; a fallback when referralFacility is blank. */
  externalProviderName: string | null;
  referralReason: string | null;
  timeOfDeath: string | null;
  causeOfDeath: string | null;
  authoredByUsername: string | null;
  authoredAt: string | null;
  approvedByUsername: string | null;
  approvedAt: string | null;
}

/**
 * Printable closure document (DISCH-2): Discharge Summary, Referral Letter or
 * Death Record, rendered from a closure plan's structured fields. Opened in an
 * NgbModal from the discharge-plan / consultation-closure modals via a "Print"
 * action. Uses the shared {@link PrintLayoutComponent} letterhead.
 */
@Component({
  selector: 'app-closure-document',
  standalone: true,
  imports: [CommonModule, PrintLayoutComponent],
  templateUrl: './closure-document.component.html'
})
export class ClosureDocumentComponent {
  @Input({ required: true }) data!: ClosureDocumentData;

  protected readonly activeModal = inject(NgbActiveModal);

  get title(): string {
    switch (this.data.kind) {
      case 'DISCHARGE': return 'Discharge Summary';
      case 'REFERRAL':  return 'Referral Letter';
      case 'DECEASED':  return 'Death Record';
      default:          return 'Closure Document';
    }
  }

  get meta(): PrintMeta[] {
    const d = this.data;
    return [
      { label: 'Patient', value: d.patientName },
      { label: 'Patient No', value: d.patientNo },
      { label: d.encounterLabel, value: d.encounterNo },
      { label: 'Status', value: d.status },
      { label: 'Authored by', value: d.authoredByUsername },
      { label: 'Approved by', value: d.approvedByUsername }
    ];
  }

  /** Narrative sections rendered in order; blanks are dropped. */
  get sections(): { label: string; value: string | null }[] {
    const d = this.data;
    return [
      { label: 'History', value: d.history },
      { label: 'Investigation', value: d.investigation },
      { label: 'Management', value: d.management },
      { label: 'Operation note', value: d.operationNote },
      { label: 'ICU note', value: d.icuNote },
      { label: 'Recommendations', value: d.recommendations }
    ].filter((s) => !!s.value);
  }
}
