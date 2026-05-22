/** A clinician affiliated with a clinic (md_clinic_clinician). */
export interface ClinicClinician {
  uid: string;
  clinicUid: string;
  userUid: string;
  username: string;
  fullName: string;
  specialty: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
