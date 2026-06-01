import { Routes } from '@angular/router';

export const REPORTING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./reporting-hub.component').then((m) => m.ReportingHubComponent),
    data: { title: 'Reports' }
  },
  {
    path: 'revenue',
    loadComponent: () =>
      import('./revenue-report.component').then((m) => m.RevenueReportComponent),
    data: { title: 'Revenue summary' }
  },
  {
    path: 'revenue-by-mode',
    loadComponent: () =>
      import('./revenue-by-mode-report.component').then((m) => m.RevenueByModeReportComponent),
    data: { title: 'Revenue by payment mode' }
  },
  {
    path: 'collections',
    loadComponent: () =>
      import('./collections-report.component').then((m) => m.CollectionsReportComponent),
    data: { title: 'Collections / cash-up' }
  },
  {
    path: 'pharmacy-sales',
    loadComponent: () =>
      import('./pharmacy-sales-report.component').then((m) => m.PharmacySalesReportComponent),
    data: { title: 'Pharmacy sales' }
  },
  {
    path: 'ipd-register',
    loadComponent: () =>
      import('./ipd-register-report.component').then((m) => m.IpdRegisterReportComponent),
    data: { title: 'IPD register' }
  },
  {
    path: 'bed-occupancy',
    loadComponent: () =>
      import('./bed-occupancy-report.component').then((m) => m.BedOccupancyReportComponent),
    data: { title: 'Bed occupancy' }
  },
  {
    path: 'stock-out',
    loadComponent: () =>
      import('./stock-out-report.component').then((m) => m.StockOutReportComponent),
    data: { title: 'Stock-out' }
  },
  {
    path: 'expiring-batches',
    loadComponent: () =>
      import('./expiring-batches-report.component').then((m) => m.ExpiringBatchesReportComponent),
    data: { title: 'Expiring batches' }
  }
];
