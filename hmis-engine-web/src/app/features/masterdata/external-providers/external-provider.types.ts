export interface ExternalMedicalProvider {
  uid: string;
  code: string;
  name: string;
  address: string | null;
  telephone: string | null;
  email: string | null;
  fax: string | null;
  website: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateExternalMedicalProviderRequest {
  code: string;
  name: string;
  address: string | null;
  telephone: string | null;
  email: string | null;
  fax: string | null;
  website: string | null;
}

export interface UpdateExternalMedicalProviderRequest {
  name: string;
  address: string | null;
  telephone: string | null;
  email: string | null;
  fax: string | null;
  website: string | null;
}

export interface ExternalMedicalProviderSearchParams {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
