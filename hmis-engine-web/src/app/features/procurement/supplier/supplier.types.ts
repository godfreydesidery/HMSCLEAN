export interface Supplier {
  uid: string;
  code: string;
  name: string;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  taxId: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierRequest {
  code: string;
  name: string;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  taxId: string | null;
  notes: string | null;
}

export interface UpdateSupplierRequest {
  name: string;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  taxId: string | null;
  notes: string | null;
}

export interface SupplierSearchParams {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
