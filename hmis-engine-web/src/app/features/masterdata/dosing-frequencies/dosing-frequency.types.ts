export interface DosingFrequency {
  uid: string;
  code: string;
  name: string;
  timesPerDay: number | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDosingFrequencyRequest { code: string; name: string; timesPerDay: number | null; description: string | null; }
export interface UpdateDosingFrequencyRequest { name: string; timesPerDay: number | null; description: string | null; }
export interface DosingFrequencySearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
