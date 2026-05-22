export interface Currency {
  uid: string;
  code: string;
  name: string;
  symbol: string | null;
  isDefault: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCurrencyRequest {
  code: string;
  name: string;
  symbol: string | null;
  makeDefault: boolean;
}

export interface UpdateCurrencyRequest {
  name: string;
  symbol: string | null;
}

export interface CurrencySearchParams {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
