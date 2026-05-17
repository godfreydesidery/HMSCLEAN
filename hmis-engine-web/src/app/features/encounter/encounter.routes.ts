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
  }
];
