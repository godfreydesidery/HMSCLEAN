export type RadiologyModality =
  | 'X_RAY' | 'CT_SCAN' | 'MRI' | 'ULTRASOUND' | 'MAMMOGRAPHY' | 'FLUOROSCOPY' | 'NUCLEAR' | 'PET';

export const RADIOLOGY_MODALITIES: { value: RadiologyModality; label: string }[] = [
  { value: 'X_RAY',       label: 'X-Ray' },
  { value: 'CT_SCAN',     label: 'CT Scan' },
  { value: 'MRI',         label: 'MRI' },
  { value: 'ULTRASOUND',  label: 'Ultrasound' },
  { value: 'MAMMOGRAPHY', label: 'Mammography' },
  { value: 'FLUOROSCOPY', label: 'Fluoroscopy' },
  { value: 'NUCLEAR',     label: 'Nuclear medicine' },
  { value: 'PET',         label: 'PET' }
];

export interface RadiologyType {
  uid: string;
  code: string;
  name: string;
  modality: RadiologyModality;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRadiologyTypeRequest { code: string; name: string; modality: RadiologyModality; description: string | null; }
export interface UpdateRadiologyTypeRequest { name: string; modality: RadiologyModality; description: string | null; }
export interface RadiologyTypeSearchParams { query?: string; active?: boolean; modality?: RadiologyModality; page?: number; size?: number; sort?: string; }
