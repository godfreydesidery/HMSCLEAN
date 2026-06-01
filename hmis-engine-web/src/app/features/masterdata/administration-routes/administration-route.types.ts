export interface AdministrationRoute {
  uid: string;
  code: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAdministrationRouteRequest { code: string; name: string; description: string | null; }
export interface UpdateAdministrationRouteRequest { name: string; description: string | null; }
export interface AdministrationRouteSearchParams { query?: string; active?: boolean; page?: number; size?: number; sort?: string; }
