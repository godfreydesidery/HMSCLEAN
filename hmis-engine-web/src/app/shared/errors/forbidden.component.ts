import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="forbidden">
      <h1>403 — Access denied</h1>
      <p>You do not have permission to view that page.</p>
      <a routerLink="/dashboard">Return to dashboard</a>
    </section>
  `,
  styles: [`
    .forbidden {
      max-width: 480px;
      margin: 4rem auto;
      text-align: center;
      background: #fff;
      border-radius: 10px;
      padding: 2rem;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
    }
    h1 { margin: 0 0 0.75rem; color: #b71c1c; }
    p  { margin: 0 0 1rem; color: #44525f; }
    a  { color: #1f6feb; text-decoration: none; font-weight: 600; }
  `]
})
export class ForbiddenComponent {}
