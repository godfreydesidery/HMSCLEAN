export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface UserSummary {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string | null;
  enabled: boolean;
}

export interface LoginResponse {
  tokens: TokenPair;
  user: UserSummary;
  roles: string[];
  privileges: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}
