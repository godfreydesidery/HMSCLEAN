export type WardCategory =
  | 'GENERAL'
  | 'PRIVATE'
  | 'SEMI_PRIVATE'
  | 'ICU'
  | 'HDU'
  | 'ISOLATION'
  | 'MATERNITY'
  | 'PEDIATRIC';

export const WARD_CATEGORIES: { value: WardCategory; label: string }[] = [
  { value: 'GENERAL',      label: 'General' },
  { value: 'PRIVATE',      label: 'Private' },
  { value: 'SEMI_PRIVATE', label: 'Semi-private' },
  { value: 'ICU',          label: 'ICU' },
  { value: 'HDU',          label: 'HDU' },
  { value: 'ISOLATION',    label: 'Isolation' },
  { value: 'MATERNITY',    label: 'Maternity' },
  { value: 'PEDIATRIC',    label: 'Pediatric' }
];

export interface Ward {
  uid: string;
  code: string;
  name: string;
  category: WardCategory;
  capacity: number;
  location: string | null;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWardRequest {
  code: string;
  name: string;
  category: WardCategory;
  capacity: number;
  location: string | null;
  description: string | null;
}

export interface UpdateWardRequest {
  name: string;
  category: WardCategory;
  capacity: number;
  location: string | null;
  description: string | null;
}

export interface WardSearchParams {
  query?: string;
  active?: boolean;
  category?: WardCategory;
  page?: number;
  size?: number;
  sort?: string;
}
