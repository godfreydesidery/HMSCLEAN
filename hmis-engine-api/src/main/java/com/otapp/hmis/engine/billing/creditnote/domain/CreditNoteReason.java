package com.otapp.hmis.engine.billing.creditnote.domain;

/**
 * Why a write-down was raised against an invoice (PROCESS.md §11, §16).
 * Reported separately from cash collections so finance can see goodwill
 * gestures, hardship waivers, and correcting entries as their own line
 * in the revenue mix.
 */
public enum CreditNoteReason {
    HARDSHIP,
    GOODWILL,
    ERROR_CORRECTION,
    SERVICE_NOT_RENDERED,
    ROUNDING,
    OTHER
}
