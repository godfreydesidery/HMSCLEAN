export type MedicineForm =
  | 'TABLET' | 'CAPSULE' | 'SYRUP' | 'SUSPENSION' | 'INJECTION' | 'INFUSION'
  | 'CREAM' | 'OINTMENT' | 'DROPS' | 'INHALER' | 'SUPPOSITORY' | 'POWDER' | 'PATCH' | 'OTHER';

export const MEDICINE_FORMS: { value: MedicineForm; label: string }[] = [
  { value: 'TABLET',      label: 'Tablet' },
  { value: 'CAPSULE',     label: 'Capsule' },
  { value: 'SYRUP',       label: 'Syrup' },
  { value: 'SUSPENSION',  label: 'Suspension' },
  { value: 'INJECTION',   label: 'Injection' },
  { value: 'INFUSION',    label: 'Infusion' },
  { value: 'CREAM',       label: 'Cream' },
  { value: 'OINTMENT',    label: 'Ointment' },
  { value: 'DROPS',       label: 'Drops' },
  { value: 'INHALER',     label: 'Inhaler' },
  { value: 'SUPPOSITORY', label: 'Suppository' },
  { value: 'POWDER',      label: 'Powder' },
  { value: 'PATCH',       label: 'Patch' },
  { value: 'OTHER',       label: 'Other' }
];

export interface Medicine {
  uid: string;
  code: string;
  name: string;
  genericName: string | null;
  strength: string | null;
  form: MedicineForm;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMedicineRequest {
  code: string; name: string; genericName: string | null; strength: string | null;
  form: MedicineForm; description: string | null;
}

export interface UpdateMedicineRequest {
  name: string; genericName: string | null; strength: string | null;
  form: MedicineForm; description: string | null;
}

export interface MedicineSearchParams { query?: string; active?: boolean; form?: MedicineForm; page?: number; size?: number; sort?: string; }

export interface MedicineUnit {
  uid: string;
  medicineUid: string;
  code: string;
  name: string;
  factorToBase: number;
  base: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
