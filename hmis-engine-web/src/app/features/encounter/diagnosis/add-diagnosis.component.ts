import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { finalize } from 'rxjs';

import { DiagnosisTypeService } from '../../masterdata/diagnoses/diagnosis.service';
import { DiagnosisType } from '../../masterdata/diagnoses/diagnosis.types';
import { ConsultationDiagnosisService } from './consultation-diagnosis.service';
import { DIAGNOSIS_KINDS, DiagnosisKind } from './consultation-diagnosis.types';

@Component({
  selector: 'app-add-diagnosis',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-diagnosis.component.html'
})
export class AddDiagnosisComponent implements OnInit {
  @Input({ required: true }) consultationUid!: string;
  @Input() initialKind: DiagnosisKind = 'WORKING';

  private readonly fb = inject(FormBuilder);
  private readonly diagnosisService = inject(ConsultationDiagnosisService);
  private readonly diagnosisTypeService = inject(DiagnosisTypeService);
  protected readonly activeModal = inject(NgbActiveModal);

  readonly kinds = DIAGNOSIS_KINDS;
  readonly diagnosisTypes = signal<DiagnosisType[]>([]);
  readonly loadingTypes = signal(false);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    kind: ['WORKING' as DiagnosisKind, [Validators.required]],
    diagnosisTypeUid: ['', [Validators.required]],
    primaryDiagnosis: [false],
    notes: ['', [Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    this.form.controls.kind.setValue(this.initialKind);
    this.loadingTypes.set(true);
    this.diagnosisTypeService.search({ active: true, size: 300, sort: 'name,asc' })
      .pipe(finalize(() => this.loadingTypes.set(false))).subscribe({
        next: (res) => this.diagnosisTypes.set(res.content),
        error: () => this.errorMessage.set('Could not load diagnosis catalogue.')
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) { this.form.markAllAsTouched(); return; }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.diagnosisService.add(this.consultationUid, {
      kind: raw.kind,
      diagnosisTypeUid: raw.diagnosisTypeUid,
      primaryDiagnosis: raw.primaryDiagnosis,
      notes: raw.notes?.trim() || null
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (d) => this.activeModal.close(d),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not add diagnosis.')
    });
  }
}
