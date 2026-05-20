import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

import { PatientService } from '../../features/patient/patient.service';
import { Patient, PatientSummary } from '../../features/patient/patient.types';

/**
 * Reusable patient picker. Clinicians search by name or patient number — they
 * never type a uid (see the no-uid-in-ui rule). Emits the full {@link Patient}
 * on selection (and on preload via {@link initialUid}); the uid stays internal.
 */
@Component({
  selector: 'app-patient-search',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patient-search.component.html'
})
export class PatientSearchComponent implements OnInit {
  /** Preload a patient (e.g. from a ?patientUid deep-link) without making the user search. */
  @Input() initialUid: string | null = null;
  @Input() invalid = false;
  @Output() readonly selectedChange = new EventEmitter<Patient | null>();

  private readonly patientService = inject(PatientService);

  readonly query = new FormControl('', { nonNullable: true });
  readonly results = signal<PatientSummary[]>([]);
  readonly searching = signal(false);
  readonly searched = signal(false);
  readonly selected = signal<Patient | null>(null);

  constructor() {
    this.query.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap((q) => { if (q.trim().length < 2) { this.results.set([]); this.searched.set(false); } }),
      switchMap((q) => {
        const term = q.trim();
        if (term.length < 2) return of(null);
        this.searching.set(true);
        return this.patientService.search({ query: term, active: true, size: 8, sort: 'lastName,asc' })
          .pipe(catchError(() => of(null)));
      }),
      takeUntilDestroyed()
    ).subscribe((page) => {
      this.searching.set(false);
      this.searched.set(true);
      this.results.set(page?.content ?? []);
    });
  }

  ngOnInit(): void {
    if (this.initialUid) {
      this.patientService.findByUid(this.initialUid).subscribe({
        next: (p) => this.apply(p),
        error: () => { /* leave unselected; user can search */ }
      });
    }
  }

  select(s: PatientSummary): void {
    this.results.set([]);
    this.searched.set(false);
    this.patientService.findByUid(s.uid).subscribe({
      next: (p) => this.apply(p),
      error: () => { /* keep searching */ }
    });
  }

  clear(): void {
    this.selected.set(null);
    this.query.setValue('');
    this.results.set([]);
    this.searched.set(false);
    this.selectedChange.emit(null);
  }

  private apply(p: Patient): void {
    this.selected.set(p);
    this.query.setValue('', { emitEvent: false });
    this.selectedChange.emit(p);
  }
}
