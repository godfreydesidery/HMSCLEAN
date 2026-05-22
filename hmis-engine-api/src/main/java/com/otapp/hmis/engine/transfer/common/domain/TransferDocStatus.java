package com.otapp.hmis.engine.transfer.common.domain;

/**
 * Shared lifecycle for the two ordering documents (RO + TO) in any
 * inventory transfer chain — pharmacy ↔ store and pharmacy ↔ pharmacy
 * both use this enum. Mirrors the legacy Zana-HMIS gates so the same
 * supervisor / manager sign-off pattern carries over (PROCESS.md §15).
 *
 * <pre>
 *   PENDING ─► VERIFIED ─► APPROVED ─► SUBMITTED ─► IN_PROCESS ─► GOODS_ISSUED ─► COMPLETED
 *                                                                                      ▲
 *      └──────────────────────► REJECTED / RETURNED ─────────────────────────────────────┘
 * </pre>
 *
 * For the request-order side: SUBMITTED = handed to the supplier;
 * IN_PROCESS = supplier has picked it up; GOODS_ISSUED = supplier's TO
 * has shipped; COMPLETED = receiver has signed the RN.
 *
 * For the transfer-order side: IN_PROCESS = preparer is doing the picks;
 * GOODS_ISSUED = stock has left the source (balance decremented);
 * COMPLETED = receiver has signed the RN.
 */
public enum TransferDocStatus {
    PENDING,
    VERIFIED,
    APPROVED,
    SUBMITTED,
    IN_PROCESS,
    GOODS_ISSUED,
    COMPLETED,
    REJECTED,
    RETURNED
}
