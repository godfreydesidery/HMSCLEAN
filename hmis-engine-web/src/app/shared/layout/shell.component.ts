import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

interface NavItem {
  label: string;
  path: string;
  privileges: string[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly auth = inject(AuthService);

  readonly appName = environment.appName;
  readonly user = this.auth.user;

  private readonly allNavItems: NavItem[] = [
    { label: 'Dashboard',   path: '/dashboard',   privileges: [] },
    { label: 'Patients',    path: '/patients',    privileges: ['PATIENT_ACCESS'] },
    { label: 'Encounters',  path: '/encounters',  privileges: ['ENCOUNTER_ACCESS'] },
    { label: 'Orders',      path: '/orders',      privileges: ['ORDERS_ACCESS'] },
    { label: 'Pharmacy',    path: '/pharmacy',    privileges: ['PHARMACY_ACCESS'] },
    { label: 'Procurement', path: '/procurement', privileges: ['PROCUREMENT_ACCESS'] },
    { label: 'Billing',     path: '/billing',     privileges: ['BILLING_ACCESS'] },
    { label: 'HR',          path: '/hr',          privileges: ['HR_ACCESS'] },
    { label: 'Reports',     path: '/reporting',   privileges: ['REPORTING_ACCESS'] },
    { label: 'Master Data', path: '/masterdata',  privileges: ['MASTERDATA_MANAGE'] },
    { label: 'Users',       path: '/iam/users',   privileges: ['USER_READ'] },
    { label: 'Roles',       path: '/iam/roles',   privileges: ['ROLE_READ'] }
  ];

  readonly navItems = computed<NavItem[]>(() =>
    this.allNavItems.filter((item) => item.privileges.length === 0 || this.auth.hasPrivilege(...item.privileges))
  );

  logout(): void {
    this.auth.logout();
  }
}
