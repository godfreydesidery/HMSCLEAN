import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { LabTestTypeService } from '../../masterdata/lab-tests/lab-test.service';
import { LabTestType } from '../../masterdata/lab-tests/lab-test.types';
import { LabBatchService } from './lab-batch.service';

@Component({
  selector: 'app-lab-batch-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lab-batch-create.component.html'
})
export class LabBatchCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly labBatchService = inject(LabBatchService);
  private readonly labTestService = inject(LabTestTypeService);
  private readonly router = inject(Router);

  readonly labTests = signal<LabTestType[]>([]);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    labTestTypeUid: ['', [Validators.required]],
    note: ['', [Validators.maxLength(500)]],
    /** One uid per line; trimmed + split on submit. */
    orderUidsBlob: ['', [Validators.required]]
  });

  ngOnInit(): void {
    this.labTestService.search({ active: true, size: 500, sort: 'name,asc' }).subscribe({
      next: (res) => this.labTests.set(res.content),
      error: () => { /* dropdown stays empty; user sees validation error */ }
    });
  }

  submit(): void {
    if (this.submitting()) return;
    const orderUids = parseUidLines(this.form.controls.orderUidsBlob.value);
    if (orderUids.length === 0) {
      this.form.controls.orderUidsBlob.setErrors({ required: true });
    }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.submitting.set(true);
    this.errorMessage.set(null);
    const raw = this.form.getRawValue();
    this.labBatchService.create({
      labTestTypeUid: raw.labTestTypeUid,
      note: raw.note.trim() || null,
      orderUids
    }).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (created) => void this.router.navigate(['/encounters/lab-batches', created.uid]),
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not create batch.')
    });
  }

  cancel(): void {
    void this.router.navigate(['/encounters/lab-batches']);
  }
}

function parseUidLines(blob: string | null | undefined): string[] {
  if (!blob) return [];
  return blob.split(/[\s,]+/).map((s) => s.trim()).filter((s) => s.length > 0);
}
