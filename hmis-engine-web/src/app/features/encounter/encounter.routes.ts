import { Routes } from '@angular/router';

export const ENCOUNTER_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'consultations' },
  {
    path: 'consultations',
    loadComponent: () =>
      import('./consultation/consultation-list.component').then((m) => m.ConsultationListComponent)
  },
  {
    path: 'consultations/new',
    loadComponent: () =>
      import('./consultation/start-consultation.component').then((m) => m.StartConsultationComponent)
  },
  {
    path: 'consultations/:uid',
    loadComponent: () =>
      import('./consultation/consultation-detail.component').then((m) => m.ConsultationDetailComponent)
  },
  {
    path: 'admissions',
    loadComponent: () =>
      import('./admission/admission-list.component').then((m) => m.AdmissionListComponent)
  },
  {
    path: 'admissions/new',
    loadComponent: () =>
      import('./admission/admit-patient.component').then((m) => m.AdmitPatientComponent)
  },
  {
    path: 'admissions/:uid',
    loadComponent: () =>
      import('./admission/admission-detail.component').then((m) => m.AdmissionDetailComponent)
  }
];
