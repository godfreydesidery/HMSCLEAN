export interface User {
  uid: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string | null;
  enabled: boolean;
  locked: boolean;
  passwordMustChange: boolean;
  lockedUntil: string | null;
  lastLoginAt: string | null;
  roles: string[];
}

export interface CreateUserRequest {
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email: string | null;
  roles: string[];
}

export interface UserSearchParams {
  query?: string;
  enabled?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface Role {
  uid: string;
  name: string;
  description: string | null;
  privileges: string[];
  /** System role — privileges locked (cannot be edited/deleted). */
  protected: boolean;
}
