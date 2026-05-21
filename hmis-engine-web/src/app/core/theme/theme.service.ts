import { Injectable, signal } from '@angular/core';

export interface ThemeOption {
  id: string;
  label: string;
  description: string;
  /** Whether the theme is dark (drives Bootstrap's data-bs-theme). */
  dark: boolean;
  /** A small swatch [background, accent] for the picker. */
  swatch: [string, string];
}

const STORAGE_KEY = 'hmis-theme';

/** Keep in sync with the [data-theme] blocks in styles.scss and index.html. */
export const THEMES: ThemeOption[] = [
  {
    id: 'desktop',
    label: 'Desktop',
    description: 'Native desktop app — dense & compact',
    dark: false,
    swatch: ['#eef0f3', '#d2691e']
  },
  {
    id: 'web',
    label: 'Web',
    description: 'Modern web app — clean & airy',
    dark: false,
    swatch: ['#f7f8fb', '#4f46e5']
  },
  {
    id: 'midnight',
    label: 'Midnight',
    description: 'Dark slate with amber accent',
    dark: true,
    swatch: ['#0f1216', '#f59e0b']
  }
];

const DEFAULT_THEME = 'desktop';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly themes = THEMES;
  readonly current = signal<string>(this.read());

  /** Re-apply on startup so the live attribute matches the stored value. */
  init(): void {
    this.apply(this.current());
  }

  select(id: string): void {
    if (!THEMES.some((t) => t.id === id)) {
      return;
    }
    this.apply(id);
    this.current.set(id);
    localStorage.setItem(STORAGE_KEY, id);
  }

  private apply(id: string): void {
    const theme = THEMES.find((t) => t.id === id) ?? THEMES[0];
    const root = document.documentElement;
    root.dataset['theme'] = theme.id;
    root.dataset['bsTheme'] = theme.dark ? 'dark' : 'light';
  }

  private read(): string {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved && THEMES.some((t) => t.id === saved) ? saved : DEFAULT_THEME;
  }
}
