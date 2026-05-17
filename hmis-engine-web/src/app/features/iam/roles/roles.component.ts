import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-iam-roles',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1>Roles & Privileges</h1>
    <p class="placeholder">Role / privilege administration UI will be implemented next.</p>
  `,
  styles: [`
    .placeholder { color: #6b7a87; }
  `]
})
export class RolesComponent {}
