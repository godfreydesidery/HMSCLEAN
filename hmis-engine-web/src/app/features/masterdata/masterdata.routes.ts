import { Routes } from '@angular/router';

export const MASTERDATA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./masterdata-shell.component').then((m) => m.MasterdataShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'clinics' },
      {
        path: 'clinics',
        loadComponent: () =>
          import('./clinics/clinic-list.component').then((m) => m.ClinicListComponent)
      },
      {
        path: 'wards',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Wards' }
      },
      {
        path: 'pharmacies',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Pharmacies' }
      },
      {
        path: 'stores',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Stores' }
      },
      {
        path: 'diagnoses',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Diagnoses' }
      },
      {
        path: 'lab-tests',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Lab tests' }
      },
      {
        path: 'procedures',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Procedures' }
      },
      {
        path: 'radiology',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Radiology' }
      },
      {
        path: 'medicines',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Medicines' }
      },
      {
        path: 'insurance',
        loadComponent: () =>
          import('../placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
        data: { title: 'Insurance' }
      }
    ]
  }
];
