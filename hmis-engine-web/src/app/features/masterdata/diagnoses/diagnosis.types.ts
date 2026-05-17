export interface DiagnosisType {
  uid: string;
  code: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDiagnosisTypeRequest { code: string; name: string; description: string | null; }
export interface UpdateDiagnosisTypeRequest { name: string; description: string | null; }
export interface DiagnosisTypeSearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
