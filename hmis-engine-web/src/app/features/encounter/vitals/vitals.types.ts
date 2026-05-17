export interface PatientVitals {
  uid: string;
  consultationUid: string;
  patientUid: string;
  takenAt: string;
  temperatureC: number | null;
  pulseBpm: number | null;
  respirationBpm: number | null;
  bloodPressureSystolic: number | null;
  bloodPressureDiastolic: number | null;
  spo2Percent: number | null;
  weightKg: number | null;
  heightCm: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RecordVitalsRequest {
  temperatureC: number | null;
  pulseBpm: number | null;
  respirationBpm: number | null;
  bloodPressureSystolic: number | null;
  bloodPressureDiastolic: number | null;
  spo2Percent: number | null;
  weightKg: number | null;
  heightCm: number | null;
  notes: string | null;
}
