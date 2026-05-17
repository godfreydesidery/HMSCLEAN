export interface ClinicalNote {
  uid: string;
  consultationUid: string;
  chiefComplaint: string | null;
  historyOfPresentingIllness: string | null;
  pastMedicalHistory: string | null;
  examination: string | null;
  assessment: string | null;
  plan: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SaveClinicalNoteRequest {
  chiefComplaint: string | null;
  historyOfPresentingIllness: string | null;
  pastMedicalHistory: string | null;
  examination: string | null;
  assessment: string | null;
  plan: string | null;
}
