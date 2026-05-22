import { ClinicalOrderKind, ClinicalOrderStatus, OrderUrgency } from '../encounter/order/clinical-order.types';
import { PatientClassScope } from '../../shared/patient-class/patient-class';

/** Mirrors backend `ClinicalOrderDtos.OrderWorklistDto`. */
export interface OrderWorklistRow {
  uid: string;
  orderNo: string;
  kind: ClinicalOrderKind;
  serviceCode: string | null;
  serviceName: string | null;
  status: ClinicalOrderStatus;
  urgency: OrderUrgency;
  requestedAt: string;
  completedAt: string | null;
  patientUid: string;
  patientNo: string | null;
  patientName: string | null;
  patientClass: PatientClassScope;
  settled: boolean;
  consultationUid: string | null;
}

export interface OrderWorklistParams {
  kind?: ClinicalOrderKind;
  status?: ClinicalOrderStatus;
  patientClass?: PatientClassScope;
  settledOnly?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}
