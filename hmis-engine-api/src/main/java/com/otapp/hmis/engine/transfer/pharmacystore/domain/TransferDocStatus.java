package com.otapp.hmis.engine.transfer.pharmacystore.domain;

/**
 * Shared lifecycle for the two ordering documents in the pharmacy ↔ store
 * transfer chain — the Request Order (pharmacy → store) and the Transfer
 * Order (store → pharmacy). Mirrors the legacy Zana-HMIS gates so the same
 * supervisor / manager sign-off pattern carries over (PROCESS.md §15).
 *
 * <pre>
 *   PENDING ─► VERIFIED ─► APPROVED ─► SUBMITTED ─► IN_PROCESS ─► GOODS_ISSUED ─► COMPLETED
 *                                                                                      ▲
 *      └──────────────────────► REJECTED / RETURNED ─────────────────────────────────────┘
 * </pre>
 *
 * For the request-order side: SUBMITTED = handed to the store; IN_PROCESS =
 * store has picked it up; GOODS_ISSUED = store's TO has shipped; COMPLETED =
 * receiving pharmacy has signed the RN.
 *
 * For the transfer-order side: IN_PROCESS = store-keeper is preparing the
 * picks; GOODS_ISSUED = stock has left the store (balance decremented);
 * COMPLETED = pharmacy has signed the RN.
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
