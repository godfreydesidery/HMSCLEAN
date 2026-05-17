import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';

import { AuthService } from '../../core/auth/auth.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  privileges: string[];
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet, NgbDropdownModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly auth = inject(AuthService);

  readonly user = this.auth.user;
  readonly sidebarCollapsed = signal(false);

  private readonly allGroups: NavGroup[] = [
    {
      label: 'Overview',
      items: [
        { label: 'Dashboard', path: '/dashboard', icon: 'bi-speedometer2', privileges: [] }
      ]
    },
    {
      label: 'Clinical',
      items: [
        { label: 'Patients',    path: '/patients',                icon: 'bi-people',           privileges: ['PATIENT_ACCESS'] },
        { label: 'Consultations', path: '/encounters/consultations', icon: 'bi-clipboard2-pulse', privileges: ['ENCOUNTER_ACCESS'] },
        { label: 'Admissions',  path: '/encounters/admissions',   icon: 'bi-hospital',         privileges: ['ENCOUNTER_ACCESS'] },
        { label: 'Orders',      path: '/orders',                  icon: 'bi-card-list',        privileges: ['ORDERS_ACCESS'] }
      ]
    },
    {
      label: 'Operations',
      items: [
        { label: 'Pharmacy stock', path: '/pharmacy/stock', icon: 'bi-capsule',     privileges: ['PHARMACY_ACCESS'] },
        { label: 'Pharmacy sales', path: '/pharmacy/sales', icon: 'bi-cart',        privileges: ['PHARMACY_ACCESS'] },
        { label: 'Procurement', path: '/procurement/orders',    icon: 'bi-truck',          privileges: ['PROCUREMENT_ACCESS'] },
        { label: 'Suppliers',   path: '/procurement/suppliers', icon: 'bi-buildings',      privileges: ['PROCUREMENT_ACCESS'] },
        { label: 'Billing',     path: '/billing',     icon: 'bi-cash-coin',       privileges: ['BILLING_ACCESS'] },
        { label: 'HR',          path: '/hr',          icon: 'bi-person-badge',    privileges: ['HR_ACCESS'] },
        { label: 'Reports',     path: '/reporting',   icon: 'bi-bar-chart-line',  privileges: ['REPORTING_ACCESS'] }
      ]
    },
    {
      label: 'Administration',
      items: [
        { label: 'Master Data', path: '/masterdata',  icon: 'bi-collection',       privileges: ['MASTERDATA_MANAGE'] },
        { label: 'Users',       path: '/iam/users',   icon: 'bi-person-gear',      privileges: ['USER_READ'] },
        { label: 'Roles',       path: '/iam/roles',   icon: 'bi-shield-lock',      privileges: ['ROLE_READ'] },
        { label: 'Login audit', path: '/iam/audit',   icon: 'bi-shield-check',     privileges: ['USER_READ'] }
      ]
    }
  ];

  readonly navGroups = computed<NavGroup[]>(() =>
    this.allGroups
      .map((g) => ({
        label: g.label,
        items: g.items.filter((it) => it.privileges.length === 0 || this.auth.hasPrivilege(...it.privileges))
      }))
      .filter((g) => g.items.length > 0)
  );

  readonly initials = computed(() => {
    const u = this.user();
    if (!u) {
      return '?';
    }
    const first = u.firstName?.[0] ?? '';
    const last = u.lastName?.[0] ?? '';
    return (first + last || u.username[0]).toUpperCase();
  });

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
  }

  logout(): void {
    this.auth.logout();
  }
}
