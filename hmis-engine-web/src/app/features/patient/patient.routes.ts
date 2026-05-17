import { Routes } from '@angular/router';

export const PATIENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./patient-list.component').then((m) => m.PatientListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./patient-form.component').then((m) => m.PatientFormComponent)
  },
  {
    path: ':uid',
    loadComponent: () => import('./patient-detail.component').then((m) => m.PatientDetailComponent)
  },
  {
    path: ':uid/edit',
    loadComponent: () => import('./patient-edit.component').then((m) => m.PatientEditComponent)
  }
];
