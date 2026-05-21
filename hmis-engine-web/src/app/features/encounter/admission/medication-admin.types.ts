export interface MedicationAdministration {
  uid: string;
  admissionUid: string;
  prescriptionUid: string;
  prescriptionNo: string | null;
  patientUid: string;
  medicineUid: string | null;
  medicineName: string | null;
  doseGiven: string;
  route: string | null;
  patientResponse: string | null;
  notes: string | null;
  administeredAt: string;
  administeredByUsername: string;
  createdAt: string;
}

export interface RecordAdministrationRequest {
  prescriptionUid: string;
  doseGiven: string;
  route: string | null;
  patientResponse: string | null;
  notes: string | null;
  administeredAt: string | null;
}
