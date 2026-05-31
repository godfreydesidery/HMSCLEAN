export interface Role {
  uid: string;
  name: string;
  description: string | null;
  privileges: string[];
  /** System role (e.g. ROOT/CLINICIAN/NURSE) — privileges are locked, cannot be edited or deleted. */
  protected: boolean;
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
