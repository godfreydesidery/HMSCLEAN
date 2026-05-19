export type EmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED';

export const EMPLOYMENT_STATUSES: { value: EmploymentStatus; label: string; badgeClass: string }[] = [
  { value: 'ACTIVE',     label: 'Active',     badgeClass: 'text-bg-success-subtle text-success-emphasis border border-success-subtle' },
  { value: 'ON_LEAVE',   label: 'On leave',   badgeClass: 'text-bg-info-subtle text-info-emphasis border border-info-subtle' },
  { value: 'SUSPENDED',  label: 'Suspended',  badgeClass: 'text-bg-warning-subtle text-warning-emphasis border border-warning-subtle' },
  { value: 'TERMINATED', label: 'Terminated', badgeClass: 'text-bg-light text-secondary border' }
];

export interface Employee {
  uid: string;
  employeeNo: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  fullName: string;
  gender: string | null;
  dateOfBirth: string | null;
  nationalId: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  username: string | null;
  designation: string | null;
  department: string | null;
  hireDate: string;
  employmentStatus: EmploymentStatus;
  terminationDate: string | null;
  terminationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeSummary {
  uid: string;
  employeeNo: string;
  firstName: string;
  middleName: string | null;
  lastName: string;
  designation: string | null;
  department: string | null;
  employmentStatus: EmploymentStatus;
}

export interface CreateEmployeeRequest {
  firstName: string;
  middleName: string | null;
  lastName: string;
  gender: string | null;
  dateOfBirth: string | null;
  nationalId: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  username: string | null;
  designation: string | null;
  department: string | null;
  hireDate: string;
}

export type UpdateEmployeeRequest = Omit<CreateEmployeeRequest, 'hireDate'>;

export interface TerminateEmployeeRequest {
  terminationDate: string;
  reason: string | null;
}

export interface EmployeeSearchParams {
  query?: string;
  status?: EmploymentStatus;
  designation?: string;
  department?: string;
  page?: number;
  size?: number;
  sort?: string;
}
