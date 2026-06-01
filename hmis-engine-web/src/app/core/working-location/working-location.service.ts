import { Injectable, signal } from '@angular/core';

/** A working location reference (pharmacy or store) — just what the UI needs to scope + label. */
export interface WorkingLocationRef {
  uid: string;
  name: string;
}

const PHARMACY_KEY = 'hmis.workingPharmacy';
const STORE_KEY = 'hmis.workingStore';

/**
 * The sticky "which pharmacy / store am I operating in" workspace — the single source
 * of truth for the legacy "Select pharmacy / Select store first, then work within it"
 * model. Signal-based and persisted to localStorage so it survives reloads (an
 * improvement over legacy, which stashed it in the session); revisiting the Select
 * page changes it without a re-login.
 */
@Injectable({ providedIn: 'root' })
export class WorkingLocationService {
  readonly workingPharmacy = signal<WorkingLocationRef | null>(this.read(PHARMACY_KEY));
  readonly workingStore = signal<WorkingLocationRef | null>(this.read(STORE_KEY));

  setPharmacy(ref: WorkingLocationRef): void {
    this.workingPharmacy.set(ref);
    this.write(PHARMACY_KEY, ref);
  }

  setStore(ref: WorkingLocationRef): void {
    this.workingStore.set(ref);
    this.write(STORE_KEY, ref);
  }

  clearPharmacy(): void {
    this.workingPharmacy.set(null);
    localStorage.removeItem(PHARMACY_KEY);
  }

  clearStore(): void {
    this.workingStore.set(null);
    localStorage.removeItem(STORE_KEY);
  }

  private read(key: string): WorkingLocationRef | null {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as Partial<WorkingLocationRef>;
      return parsed && parsed.uid && parsed.name ? { uid: parsed.uid, name: parsed.name } : null;
    } catch {
      return null;
    }
  }

  private write(key: string, ref: WorkingLocationRef): void {
    localStorage.setItem(key, JSON.stringify({ uid: ref.uid, name: ref.name }));
  }
}
