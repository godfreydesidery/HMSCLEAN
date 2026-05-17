import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="placeholder">
      <h1>{{ title() }}</h1>
      <p>Module not yet implemented. Coming soon.</p>
    </section>
  `,
  styles: [`
    .placeholder {
      background: #fff;
      border: 1px dashed #c8d2dc;
      border-radius: 10px;
      padding: 2rem;
      color: #44525f;
    }
    h1 { margin: 0 0 0.5rem; color: #1f2933; }
    p  { margin: 0; color: #6b7a87; }
  `]
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);

  readonly title = toSignal(
    this.route.data.pipe(map((d) => (d['title'] as string) ?? 'Module')),
    { initialValue: 'Module' }
  );
}
