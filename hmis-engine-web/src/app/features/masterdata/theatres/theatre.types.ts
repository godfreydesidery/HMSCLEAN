/** Mirrors backend `TheatreDtos` — an operating theatre (masterdata). */

export interface Theatre {
  uid: string;
  code: string;
  name: string;
  location: string | null;
  description: string | null;
  active: boolean;
}

export interface CreateTheatreRequest {
  code: string;
  name: string;
  location: string | null;
  description: string | null;
}

export interface UpdateTheatreRequest {
  name: string;
  location: string | null;
  description: string | null;
}

export interface TheatreSearchParams {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
