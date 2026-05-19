/**
 * Mirrors backend `OrderAttachmentDto` (Phase 39). One row per file
 * uploaded against a clinical order — lab result PDF, radiology image,
 * intra-op photo, etc.
 */

export type ClinicalOrderKind = 'LAB_TEST' | 'RADIOLOGY' | 'PROCEDURE';

export interface OrderAttachment {
  uid: string;
  orderUid: string;
  orderKind: ClinicalOrderKind;
  filename: string;
  contentType: string;
  sizeBytes: number;
  description: string | null;
  uploadedByUsername: string;
  uploadedAt: string;
  createdAt: string;
}
