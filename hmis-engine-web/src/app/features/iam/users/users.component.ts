import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

@Component({
  selector: 'app-iam-users',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1>Users</h1>
    <p class="placeholder">User administration UI will be implemented next.</p>
  `,
  styles: [`
    .placeholder { color: #6b7a87; }
  `]
})
export class UsersComponent {}
