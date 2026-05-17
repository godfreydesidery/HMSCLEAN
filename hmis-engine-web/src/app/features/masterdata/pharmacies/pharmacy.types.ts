export interface Pharmacy {
  uid: string;
  code: string;
  name: string;
  location: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePharmacyRequest {
  code: string;
  name: string;
  location: string | null;
  description: string | null;
}

export interface UpdatePharmacyRequest {
  name: string;
  location: string | null;
  description: string | null;
}

export interface PharmacySearchParams {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
