import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { AuthService } from '../../../core/auth/auth.service';

interface StatCard {
  label: string;
  value: string;
  icon: string;
  bg: string;
  color: string;
  trend: number;
  trendLabel: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);

  readonly user = this.auth.user;
  readonly roles = this.auth.roles;
  readonly privileges = this.auth.privileges;

  readonly today = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  /**
   * Placeholder figures so the dashboard reads as "real" while the reporting
   * module is still empty. Replace with live data once that module ships.
   */
  readonly stats: StatCard[] = [
    {
      label: 'Patients today',
      value: '0',
      icon: 'bi-people',
      bg: 'rgba(37, 99, 235, 0.10)',
      color: '#2563eb',
      trend: 0,
      trendLabel: 'No data yet'
    },
    {
      label: 'Active consultations',
      value: '0',
      icon: 'bi-clipboard2-pulse',
      bg: 'rgba(16, 185, 129, 0.12)',
      color: '#059669',
      trend: 0,
      trendLabel: 'No data yet'
    },
    {
      label: 'Pending prescriptions',
      value: '0',
      icon: 'bi-capsule',
      bg: 'rgba(217, 119, 6, 0.12)',
      color: '#b45309',
      trend: 0,
      trendLabel: 'No data yet'
    },
    {
      label: 'Revenue (today)',
      value: '—',
      icon: 'bi-cash-coin',
      bg: 'rgba(99, 102, 241, 0.12)',
      color: '#4f46e5',
      trend: 0,
      trendLabel: 'No data yet'
    }
  ];
}
