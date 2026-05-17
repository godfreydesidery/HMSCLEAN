export interface ProcedureType {
  uid: string;
  code: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProcedureTypeRequest { code: string; name: string; description: string | null; }
export interface UpdateProcedureTypeRequest { name: string; description: string | null; }
export interface ProcedureTypeSearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
