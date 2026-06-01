import { Routes } from '@angular/router';

export const ENCOUNTER_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'consultations' },
  {
    path: 'reception-queue',
    loadComponent: () =>
      import('./consultation/reception-queue.component').then((m) => m.ReceptionQueueComponent)
  },
  {
    path: 'nurse-queue',
    loadComponent: () =>
      import('./nurse-queue/nurse-queue.component').then((m) => m.NurseQueueComponent)
  },
  {
    path: 'transfer-queue',
    loadComponent: () =>
      import('./consultation/transfer-queue.component').then((m) => m.TransferQueueComponent)
  },
  {
    path: 'vitals-queue',
    loadComponent: () =>
      import('./vitals/vitals-queue.component').then((m) => m.VitalsQueueComponent)
  },
  {
    path: 'closure-worklist',
    loadComponent: () =>
      import('./closure-worklist/closure-worklist.component').then((m) => m.ClosureWorklistComponent)
  },
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
  },
  {
    path: 'lab-batches',
    loadChildren: () => import('./lab-batch/lab-batch.routes').then((m) => m.LAB_BATCH_ROUTES)
  },
  {
    path: 'orders/:orderUid/operative-record',
    loadComponent: () =>
      import('./operative/operative-record.component').then((m) => m.OperativeRecordComponent)
  }
];
