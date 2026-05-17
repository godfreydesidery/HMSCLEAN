export interface Role {
  uid: string;
  name: string;
  description: string | null;
  privileges: string[];
}

export interface Privilege {
  uid: string;
  name: string;
  description: string | null;
}

export interface CreateRoleRequest {
  name: string;
  description: string | null;
  privileges: string[];
}
