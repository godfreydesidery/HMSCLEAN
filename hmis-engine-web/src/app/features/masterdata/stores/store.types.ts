export interface Store {
  uid: string;
  code: string;
  name: string;
  location: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStoreRequest {
  code: string; name: string; location: string | null; description: string | null;
}

export interface UpdateStoreRequest {
  name: string; location: string | null; description: string | null;
}

export interface StoreSearchParams {
  query?: string; active?: boolean; page?: number; size?: number; sort?: string;
}
