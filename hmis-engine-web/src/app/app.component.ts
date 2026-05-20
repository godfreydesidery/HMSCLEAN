import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, OnDestroy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Selector for fields that should not show the browser's saved-value /
 * autofill dropdown. Password inputs are deliberately excluded so password
 * managers still work on the login screen. Anything that already declares an
 * `autocomplete` attribute is left as-is.
 */
const NO_AUTOFILL = 'input:not([autocomplete]):not([type=password]), textarea:not([autocomplete]), select:not([autocomplete]), form:not([autocomplete])';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements AfterViewInit, OnDestroy {
  title = 'hmis-engine-web';

  private readonly doc = inject(DOCUMENT);
  private observer?: MutationObserver;

  ngAfterViewInit(): void {
    this.disableAutofill(this.doc);
    // Forms render lazily (router navigation, modals), so keep tagging new nodes.
    this.observer = new MutationObserver((mutations) => {
      for (const m of mutations) {
        m.addedNodes.forEach((node) => {
          if (node instanceof Element) this.disableAutofill(node);
        });
      }
    });
    this.observer.observe(this.doc.body, { childList: true, subtree: true });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private disableAutofill(root: Document | Element): void {
    if (root instanceof Element && root.matches?.(NO_AUTOFILL)) {
      root.setAttribute('autocomplete', 'off');
    }
    root.querySelectorAll?.(NO_AUTOFILL).forEach((el) => el.setAttribute('autocomplete', 'off'));
  }
}
