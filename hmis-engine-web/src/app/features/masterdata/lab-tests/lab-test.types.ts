export interface LabTestType {
  uid: string;
  code: string;
  name: string;
  specimen: string | null;
  unit: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLabTestTypeRequest {
  code: string; name: string; specimen: string | null; unit: string | null; description: string | null;
}

export interface UpdateLabTestTypeRequest {
  name: string; specimen: string | null; unit: string | null; description: string | null;
}

export interface LabTestTypeSearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
