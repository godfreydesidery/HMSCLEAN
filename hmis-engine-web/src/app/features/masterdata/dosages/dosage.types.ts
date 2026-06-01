export interface Dosage {
  uid: string;
  code: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDosageRequest { code: string; name: string; description: string | null; }
export interface UpdateDosageRequest { name: string; description: string | null; }
export interface DosageSearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
