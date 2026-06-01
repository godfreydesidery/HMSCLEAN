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
        loadComponent: () => import('./clinics/clinic-list.component').then((m) => m.ClinicListComponent)
      },
      {
        path: 'wards',
        loadComponent: () => import('./wards/ward-list.component').then((m) => m.WardListComponent)
      },
      {
        path: 'theatres',
        loadComponent: () => import('./theatres/theatre-list.component').then((m) => m.TheatreListComponent)
      },
      {
        path: 'pharmacies',
        loadComponent: () => import('./pharmacies/pharmacy-list.component').then((m) => m.PharmacyListComponent)
      },
      {
        path: 'stores',
        loadComponent: () => import('./stores/store-list.component').then((m) => m.StoreListComponent)
      },
      {
        path: 'diagnoses',
        loadComponent: () => import('./diagnoses/diagnosis-list.component').then((m) => m.DiagnosisListComponent)
      },
      {
        path: 'external-providers',
        loadComponent: () => import('./external-providers/external-provider-list.component').then((m) => m.ExternalProviderListComponent)
      },
      {
        path: 'lab-tests',
        loadComponent: () => import('./lab-tests/lab-test-list.component').then((m) => m.LabTestListComponent)
      },
      {
        path: 'procedures',
        loadComponent: () => import('./procedures/procedure-list.component').then((m) => m.ProcedureListComponent)
      },
      {
        path: 'radiology',
        loadComponent: () => import('./radiology/radiology-list.component').then((m) => m.RadiologyListComponent)
      },
      {
        path: 'medicines',
        loadComponent: () => import('./medicines/medicine-list.component').then((m) => m.MedicineListComponent)
      },
      {
        path: 'insurance',
        loadComponent: () => import('./insurance/insurance-list.component').then((m) => m.InsuranceListComponent)
      },
      {
        path: 'insurance-plans',
        loadComponent: () => import('./insurance-plans/insurance-plan-list.component').then((m) => m.InsurancePlanListComponent)
      },
      {
        path: 'pricing',
        loadComponent: () => import('./pricing/price-list.component').then((m) => m.PriceListComponent)
      },
      {
        path: 'currencies',
        loadComponent: () => import('./currencies/currency-list.component').then((m) => m.CurrencyListComponent)
      },
      {
        path: 'consumables',
        loadComponent: () => import('../consumables/consumable-list.component').then((m) => m.ConsumableListComponent)
      },
      {
        path: 'dosing-frequencies',
        loadComponent: () => import('./dosing-frequencies/dosing-frequency-list.component').then((m) => m.DosingFrequencyListComponent)
      },
      {
        path: 'dosages',
        loadComponent: () => import('./dosages/dosage-list.component').then((m) => m.DosageListComponent)
      },
      {
        path: 'administration-routes',
        loadComponent: () => import('./administration-routes/administration-route-list.component').then((m) => m.AdministrationRouteListComponent)
      }
    ]
  }
];
