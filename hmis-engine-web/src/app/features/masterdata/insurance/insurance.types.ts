export interface InsuranceProvider {
  uid: string;
  code: string;
  name: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInsuranceProviderRequest {
  code: string; name: string;
  contactPerson: string | null; phone: string | null; email: string | null;
  address: string | null; description: string | null;
}

export interface UpdateInsuranceProviderRequest {
  name: string;
  contactPerson: string | null; phone: string | null; email: string | null;
  address: string | null; description: string | null;
}

export interface InsuranceProviderSearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
