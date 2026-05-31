import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

import { AuthService } from '../../../core/auth/auth.service';
import { PageResponse } from '../../../core/http/page.types';
import { InvoiceService } from '../../billing/invoice.service';
import { AdmissionService } from '../../encounter/admission/admission.service';
import { AdmissionSummary } from '../../encounter/admission/admission.types';
import { ConsultationService } from '../../encounter/consultation/consultation.service';
import { ConsultationSummary } from '../../encounter/consultation/consultation.types';
import { PatientService } from '../../patient/patient.service';
import { PurchaseOrderService } from '../../procurement/order/purchase-order.service';

interface StatCard {
  label: string;
  value: string | number;
  icon: string;
  bg: string;
  color: string;
  link?: string;
  visible: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly patientService = inject(PatientService);
  private readonly consultationService = inject(ConsultationService);
  private readonly admissionService = inject(AdmissionService);
  private readonly invoiceService = inject(InvoiceService);
  private readonly purchaseOrderService = inject(PurchaseOrderService);

  readonly user = this.auth.user;
  readonly roles = this.auth.roles;
  readonly privileges = this.auth.privileges;

  readonly today = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly totalPatients = signal(0);
  readonly activeConsultations = signal(0);
  readonly bookedConsultations = signal(0);
  readonly activeAdmissions = signal(0);
  readonly issuedInvoices = signal(0);
  readonly partiallyPaidInvoices = signal(0);
  readonly openPurchaseOrders = signal(0);

  readonly recentConsultations = signal<ConsultationSummary[]>([]);
  readonly recentAdmissions = signal<AdmissionSummary[]>([]);

  readonly canPatient = computed(() => this.auth.hasPrivilege('PATIENT_ACCESS'));
  readonly canEncounter = computed(() => this.auth.hasPrivilege('ENCOUNTER_ACCESS'));
  readonly canBilling = computed(() => this.auth.hasPrivilege('BILLING_ACCESS'));
  readonly canProcurement = computed(() => this.auth.hasPrivilege('PROCUREMENT_ACCESS'));

  readonly stats = computed<StatCard[]>(() => [
    {
      label: 'Patients (total)',
      value: this.totalPatients(),
      icon: 'bi-people',
      bg: 'rgba(37, 99, 235, 0.10)',
      color: '#2563eb',
      link: '/patients',
      visible: this.canPatient()
    },
    {
      label: 'Active consultations',
      value: this.activeConsultations(),
      icon: 'bi-clipboard2-pulse',
      bg: 'rgba(16, 185, 129, 0.12)',
      color: '#059669',
      link: '/encounters/consultations',
      visible: this.canEncounter()
    },
    {
      label: 'Active admissions',
      value: this.activeAdmissions(),
      icon: 'bi-hospital',
      bg: 'rgba(99, 102, 241, 0.12)',
      color: '#4f46e5',
      link: '/encounters/admissions',
      visible: this.canEncounter()
    },
    {
      label: 'Open invoices',
      value: this.issuedInvoices() + this.partiallyPaidInvoices(),
      icon: 'bi-cash-coin',
      bg: 'rgba(217, 119, 6, 0.12)',
      color: '#b45309',
      link: '/billing',
      visible: this.canBilling()
    },
    {
      label: 'Open POs',
      value: this.openPurchaseOrders(),
      icon: 'bi-truck',
      bg: 'rgba(139, 92, 246, 0.12)',
      color: '#7c3aed',
      link: '/procurement/orders',
      visible: this.canProcurement()
    }
  ].filter((s) => s.visible));

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const safeCount = (observable: Observable<PageResponse<unknown>>): Observable<number> =>
      observable.pipe(
        map((res) => res?.totalElements ?? 0),
        catchError(() => of(0))
      );
    const safeList = <T>(observable: Observable<PageResponse<T>>): Observable<T[]> =>
      observable.pipe(
        map((res) => res?.content ?? []),
        catchError(() => of([] as T[]))
      );

    forkJoin({
      patients:        this.canPatient() ? safeCount(this.patientService.search({ size: 1 })) : of(0),
      activeCons:      this.canEncounter() ? safeCount(this.consultationService.search({ status: 'IN_PROGRESS', size: 1 })) : of(0),
      bookedCons:      this.canEncounter() ? safeCount(this.consultationService.search({ status: 'BOOKED', size: 1 })) : of(0),
      activeAdms:      this.canEncounter() ? safeCount(this.admissionService.search({ status: 'ADMITTED', size: 1 })) : of(0),
      issued:          this.canBilling() ? safeCount(this.invoiceService.search({ status: 'ISSUED', size: 1 })) : of(0),
      partiallyPaid:   this.canBilling() ? safeCount(this.invoiceService.search({ status: 'PARTIALLY_PAID', size: 1 })) : of(0),
      ordered:         this.canProcurement() ? safeCount(this.purchaseOrderService.search({ status: 'ORDERED', size: 1 })) : of(0),
      partiallyRecv:   this.canProcurement() ? safeCount(this.purchaseOrderService.search({ status: 'PARTIALLY_RECEIVED', size: 1 })) : of(0),
      recentCons:      this.canEncounter() ? safeList<ConsultationSummary>(this.consultationService.search({ size: 5, sort: 'bookedAt,desc' })) : of([] as ConsultationSummary[]),
      recentAdms:      this.canEncounter() ? safeList<AdmissionSummary>(this.admissionService.search({ size: 5, sort: 'admittedAt,desc' })) : of([] as AdmissionSummary[])
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (r) => {
        this.totalPatients.set(r.patients);
        this.activeConsultations.set(r.activeCons);
        this.bookedConsultations.set(r.bookedCons);
        this.activeAdmissions.set(r.activeAdms);
        this.issuedInvoices.set(r.issued);
        this.partiallyPaidInvoices.set(r.partiallyPaid);
        this.openPurchaseOrders.set(r.ordered + r.partiallyRecv);
        this.recentConsultations.set(r.recentCons);
        this.recentAdmissions.set(r.recentAdms);
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'Could not load dashboard.')
    });
  }

  startConsultation(): void { void this.router.navigate(['/encounters', 'consultations', 'new']); }
  newPatient(): void { void this.router.navigate(['/patients'], { queryParams: { create: 1 } }); }
  admit(): void { void this.router.navigate(['/encounters', 'admissions', 'new']); }

  consultationStatusBadge(status: string): string {
    switch (status) {
      case 'BOOKED':      return 'text-bg-info-subtle text-info-emphasis border border-info-subtle';
      case 'IN_PROGRESS': return 'text-bg-primary-subtle text-primary border border-primary-subtle';
      case 'COMPLETED':   return 'text-bg-success-subtle text-success-emphasis border border-success-subtle';
      default:            return 'text-bg-light border';
    }
  }
  admissionStatusBadge(status: string): string {
    switch (status) {
      case 'AWAITING_DEPOSIT': return 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle';
      case 'ADMITTED':    return 'text-bg-primary-subtle text-primary border border-primary-subtle';
      case 'DISCHARGED':  return 'text-bg-success-subtle text-success-emphasis border border-success-subtle';
      case 'DECEASED':    return 'text-bg-dark-subtle text-dark-emphasis border border-dark-subtle';
      case 'TRANSFERRED': return 'text-bg-info-subtle text-info-emphasis border border-info-subtle';
      default:            return 'text-bg-light border';
    }
  }
}
