/** Optional clinical-identity sidecar for a user (iam_provider_profile). */
export interface ProviderProfile {
  uid: string;
  userUid: string;
  specialty: string | null;
  registrationNo: string | null;
  licenseNo: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertProviderProfileRequest {
  specialty: string | null;
  registrationNo: string | null;
  licenseNo: string | null;
}
