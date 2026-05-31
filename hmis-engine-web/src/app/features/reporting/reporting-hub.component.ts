import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface ReportCard {
  path: string;
  title: string;
  description: string;
  icon: string;
  iconClass: string;
}

@Component({
  selector: 'app-reporting-hub',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="hmis-page-header">
      <div>
        <h1 class="hmis-page-title">Reports</h1>
        <p class="hmis-muted mb-0 small">Operational and financial reporting across the facility.</p>
      </div>
    </div>

    <div class="row g-3">
      @for (c of cards; track c.path) {
        <div class="col-12 col-md-6 col-xl-4">
          <a class="card h-100 text-decoration-none hmis-report-card" [routerLink]="c.path">
            <div class="card-body d-flex align-items-start gap-3">
              <span class="hmis-report-card__icon {{ c.iconClass }}">
                <i class="bi {{ c.icon }}"></i>
              </span>
              <div>
                <div class="fw-semibold text-body">{{ c.title }}</div>
                <div class="hmis-muted small">{{ c.description }}</div>
              </div>
              <i class="bi bi-chevron-right hmis-muted ms-auto"></i>
            </div>
          </a>
        </div>
      }
    </div>
  `,
  styles: [`
    .hmis-report-card {
      transition: box-shadow 0.15s, transform 0.15s, border-color 0.15s;
    }
    .hmis-report-card:hover {
      border-color: #2563eb;
      box-shadow: 0 0.25rem 0.75rem rgba(15, 23, 42, 0.08);
      transform: translateY(-1px);
    }
    .hmis-report-card__icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2.5rem;
      height: 2.5rem;
      border-radius: 0.6rem;
      font-size: 1.2rem;
      flex: 0 0 auto;
    }
  `]
})
export class ReportingHubComponent {
  readonly cards: ReportCard[] = [
    {
      path: 'revenue',
      title: 'Revenue summary',
      description: 'Billed, collected, credited and refunded over a date range, by service kind.',
      icon: 'bi-cash-coin',
      iconClass: 'text-bg-success-subtle text-success-emphasis'
    },
    {
      path: 'ipd-register',
      title: 'IPD register',
      description: 'Inpatient admissions over a period, filtered by ward and status.',
      icon: 'bi-clipboard2-pulse',
      iconClass: 'text-bg-primary-subtle text-primary'
    },
    {
      path: 'bed-occupancy',
      title: 'Bed occupancy',
      description: 'Live ward capacity, occupied, free and out-of-service beds.',
      icon: 'bi-hospital',
      iconClass: 'text-bg-info-subtle text-info-emphasis'
    },
    {
      path: 'stock-out',
      title: 'Stock-out',
      description: 'Medicines at or below a threshold across pharmacies and stores.',
      icon: 'bi-exclamation-octagon',
      iconClass: 'text-bg-danger-subtle text-danger-emphasis'
    },
    {
      path: 'expiring-batches',
      title: 'Expiring batches',
      description: 'Batches due to expire within a window, by location.',
      icon: 'bi-hourglass-split',
      iconClass: 'text-bg-warning-subtle text-warning-emphasis'
    }
  ];
}
