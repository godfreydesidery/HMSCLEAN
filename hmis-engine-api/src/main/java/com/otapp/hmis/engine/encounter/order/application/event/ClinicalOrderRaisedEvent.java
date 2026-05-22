package com.otapp.hmis.engine.encounter.order.application.event;

/**
 * Published when a consultation-bound clinical order is raised. Billing listens
 * after-commit and bills the order onto the consultation invoice up front, so a
 * CASH patient's order can be paid before the service is rendered
 * (PROCESS_MISMATCHES.md M13). Outsider-direct orders are billed via the
 * on-demand outsider invoice and do not publish this event.
 */
public record ClinicalOrderRaisedEvent(String orderUid) {}
