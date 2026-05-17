export interface InsurancePlan {
  uid: string;
  code: string;
  name: string;
  providerUid: string;
  providerName: string | null;
  coversConsultation: boolean;
  coversLab: boolean;
  coversRadiology: boolean;
  coversProcedure: boolean;
  coversMedicine: boolean;
  coversAdmission: boolean;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInsurancePlanRequest {
  code: string;
  name: string;
  providerUid: string;
  coversConsultation: boolean;
  coversLab: boolean;
  coversRadiology: boolean;
  coversProcedure: boolean;
  coversMedicine: boolean;
  coversAdmission: boolean;
  description: string | null;
}

export interface UpdateInsurancePlanRequest {
  name: string;
  coversConsultation: boolean;
  coversLab: boolean;
  coversRadiology: boolean;
  coversProcedure: boolean;
  coversMedicine: boolean;
  coversAdmission: boolean;
  description: string | null;
}

export interface InsurancePlanSearchParams {
  query?: string;
  active?: boolean;
  providerUid?: string;
  page?: number;
  size?: number;
  sort?: string;
}
