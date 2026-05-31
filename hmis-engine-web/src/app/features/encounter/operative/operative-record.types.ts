// Mirrors com.otapp.hmis.engine.encounter.operative.application.OperativeRecordDtos.
// Java Instant fields map to ISO-8601 strings (or null).

export interface OperativeRecord {
  uid: string;
  orderUid: string;
  findings: string | null;
  technique: string | null;
  instruments: string | null;
  complications: string | null;
  specimens: string | null;
  surgeonUsername: string | null;
  assistants: string | null;
  anaesthetistUsername: string | null;
  anaesthesiaType: string | null;
  scrubNurse: string | null;
  circulatingNurse: string | null;
  startedAt: string | null;
  endedAt: string | null;
  authoredByUsername: string | null;
  authoredAt: string | null;
  lockedAt: string | null;
  lockedByUsername: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/** Create-or-update the editable fields while the record is UNLOCKED. All optional. */
export interface UpsertOperativeRecordRequest {
  findings?: string | null;
  technique?: string | null;
  instruments?: string | null;
  complications?: string | null;
  specimens?: string | null;
  surgeonUsername?: string | null;
  assistants?: string | null;
  anaesthetistUsername?: string | null;
  anaesthesiaType?: string | null;
  scrubNurse?: string | null;
  circulatingNurse?: string | null;
  startedAt?: string | null;
  endedAt?: string | null;
}

/** Append-only addendum to a locked operative record. Reason required. */
export interface CreateAmendmentRequest {
  reason: string;
  findings?: string | null;
  technique?: string | null;
  instruments?: string | null;
  complications?: string | null;
  specimens?: string | null;
  assistants?: string | null;
  anaesthesiaType?: string | null;
  scrubNurse?: string | null;
  circulatingNurse?: string | null;
}

export interface OperativeRecordAmendment {
  uid: string;
  operativeRecordUid: string;
  amendmentNo: number;
  reason: string;
  findings: string | null;
  technique: string | null;
  instruments: string | null;
  complications: string | null;
  specimens: string | null;
  assistants: string | null;
  anaesthesiaType: string | null;
  scrubNurse: string | null;
  circulatingNurse: string | null;
  authoredByUsername: string | null;
  authoredAt: string | null;
}
