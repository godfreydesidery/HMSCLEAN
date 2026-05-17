import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

interface SubNavItem {
  label: string;
  path: string;
  icon: string;
}

@Component({
  selector: 'app-masterdata-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="masterdata-shell">
      <nav class="masterdata-shell__tabs" aria-label="Master data sections">
        @for (item of items; track item.path) {
          <a
            class="masterdata-shell__tab"
            [routerLink]="item.path"
            routerLinkActive="masterdata-shell__tab--active"
          >
            <i class="bi {{ item.icon }}"></i>
            <span>{{ item.label }}</span>
          </a>
        }
      </nav>
      <router-outlet />
    </div>
  `,
  styles: [`
    .masterdata-shell__tabs {
      display: flex;
      flex-wrap: wrap;
      gap: 0.25rem;
      margin-bottom: 1.25rem;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 0;
    }
    .masterdata-shell__tab {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.55rem 0.9rem;
      border-radius: 0.5rem 0.5rem 0 0;
      color: #64748b;
      text-decoration: none;
      font-size: 0.85rem;
      font-weight: 500;
      border-bottom: 2px solid transparent;
      margin-bottom: -1px;
      transition: color 0.15s, border-color 0.15s, background 0.15s;
    }
    .masterdata-shell__tab i {
      font-size: 1rem;
    }
    .masterdata-shell__tab:hover {
      color: #0f172a;
      background: #f8fafc;
    }
    .masterdata-shell__tab--active {
      color: #2563eb;
      border-bottom-color: #2563eb;
      background: #fff;
    }
  `]
})
export class MasterdataShellComponent {
  readonly items: SubNavItem[] = [
    { label: 'Clinics',          path: 'clinics',          icon: 'bi-hospital' },
    { label: 'Wards',            path: 'wards',            icon: 'bi-door-open' },
    { label: 'Pharmacies',       path: 'pharmacies',       icon: 'bi-capsule-pill' },
    { label: 'Stores',           path: 'stores',           icon: 'bi-box-seam' },
    { label: 'Diagnoses',        path: 'diagnoses',        icon: 'bi-clipboard2-check' },
    { label: 'Lab tests',        path: 'lab-tests',        icon: 'bi-droplet-half' },
    { label: 'Procedures',       path: 'procedures',       icon: 'bi-scissors' },
    { label: 'Radiology',        path: 'radiology',        icon: 'bi-radioactive' },
    { label: 'Medicines',        path: 'medicines',        icon: 'bi-capsule' },
    { label: 'Insurance',        path: 'insurance',        icon: 'bi-shield-check' }
  ];
}
