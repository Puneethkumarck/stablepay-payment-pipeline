package io.stablepay.flink.transition;

import java.util.Map;
import java.util.Set;

public final class TransitionGraph {

  private TransitionGraph() {}

  private static final Map<String, Set<String>> FIAT_PAYOUT =
      Map.ofEntries(
          Map.entry("INITIATED", Set.of("PENDING_APPROVAL", "CANCELLED")),
          Map.entry("PENDING_APPROVAL", Set.of("FIRST_APPROVAL", "CANCELLED", "EXPIRED")),
          Map.entry("FIRST_APPROVAL", Set.of("PENDING_SECOND_APPROVAL", "APPROVED")),
          Map.entry("PENDING_SECOND_APPROVAL", Set.of("APPROVED", "CANCELLED", "EXPIRED")),
          Map.entry("APPROVED", Set.of("PENDING_SCREENING")),
          Map.entry("PENDING_SCREENING", Set.of("SCREENING_IN_PROGRESS")),
          Map.entry(
              "SCREENING_IN_PROGRESS",
              Set.of(
                  "SCREENING_HOLD",
                  "SCREENING_RFI",
                  "SCREENING_RELEASED",
                  "SCREENING_REJECTED",
                  "SCREENING_SEIZED")),
          Map.entry(
              "SCREENING_HOLD",
              Set.of("SCREENING_RELEASED", "SCREENING_REJECTED", "SCREENING_SEIZED")),
          Map.entry("SCREENING_RFI", Set.of("SCREENING_RELEASED", "SCREENING_REJECTED")),
          Map.entry("SCREENING_RELEASED", Set.of("PENDING_PARTNER_ROUTING")),
          Map.entry("SCREENING_REJECTED", Set.of("FAILED", "CONFISCATED")),
          Map.entry("SCREENING_SEIZED", Set.of("CONFISCATED")),
          Map.entry("PENDING_PARTNER_ROUTING", Set.of("PARTNER_ASSIGNED")),
          Map.entry("PARTNER_ASSIGNED", Set.of("PENDING_EXECUTION")),
          Map.entry("PENDING_EXECUTION", Set.of("EXECUTING")),
          Map.entry("EXECUTING", Set.of("SENT_TO_PARTNER", "FAILED")),
          Map.entry("SENT_TO_PARTNER", Set.of("PARTNER_ACKNOWLEDGED", "FAILED")),
          Map.entry("PARTNER_ACKNOWLEDGED", Set.of("PENDING_CONFIRMATION")),
          Map.entry("PENDING_CONFIRMATION", Set.of("CONFIRMED", "FAILED")),
          Map.entry("CONFIRMED", Set.of("COMPLETED")),
          Map.entry("COMPLETED", Set.of("RETURNED", "REFUND_INITIATED")),
          Map.entry("FAILED", Set.of("MANUAL_REVIEW", "LEDGER_SUSPENSE")),
          Map.entry("RETURNED", Set.of()),
          Map.entry("REFUND_INITIATED", Set.of("REFUND_PENDING")),
          Map.entry("REFUND_PENDING", Set.of("REFUND_COMPLETED")),
          Map.entry("REFUND_COMPLETED", Set.of()),
          Map.entry("CONFISCATED", Set.of()),
          Map.entry("SUSPENDED", Set.of()),
          Map.entry("LEDGER_SUSPENSE", Set.of("MANUAL_REVIEW")),
          Map.entry("MANUAL_REVIEW", Set.of("COMPLETED", "FAILED")),
          Map.entry("CANCELLED", Set.of()),
          Map.entry("EXPIRED", Set.of()));

  private static final Map<String, Set<String>> CRYPTO_PAYOUT =
      Map.ofEntries(
          Map.entry("INITIATED", Set.of("PENDING_APPROVAL")),
          Map.entry("PENDING_APPROVAL", Set.of("APPROVED", "CANCELLED")),
          Map.entry("APPROVED", Set.of("PENDING_SCREENING")),
          Map.entry("PENDING_SCREENING", Set.of("SCREENING_IN_PROGRESS")),
          Map.entry("SCREENING_IN_PROGRESS", Set.of("SCREENING_CLEARED", "SCREENING_FLAGGED")),
          Map.entry("SCREENING_CLEARED", Set.of("PENDING_SIGNING")),
          Map.entry("SCREENING_FLAGGED", Set.of("FAILED", "CONFISCATED")),
          Map.entry("PENDING_SIGNING", Set.of("SIGNING_IN_PROGRESS")),
          Map.entry("SIGNING_IN_PROGRESS", Set.of("SIGNED", "FAILED")),
          Map.entry("SIGNED", Set.of("BROADCASTING")),
          Map.entry("BROADCASTING", Set.of("BROADCAST", "FAILED")),
          Map.entry("BROADCAST", Set.of("CONFIRMING", "STUCK")),
          Map.entry("CONFIRMING", Set.of("CONFIRMED")),
          Map.entry("CONFIRMED", Set.of("COMPLETED")),
          Map.entry("COMPLETED", Set.of()),
          Map.entry("STUCK", Set.of("RBF_INITIATED")),
          Map.entry("RBF_INITIATED", Set.of("RBF_BROADCAST")),
          Map.entry("RBF_BROADCAST", Set.of("REPLACED", "FAILED")),
          Map.entry("REPLACED", Set.of("CONFIRMING")),
          Map.entry("FAILED", Set.of()),
          Map.entry("CANCELLED", Set.of()),
          Map.entry("CONFISCATED", Set.of()));

  private static final Map<String, Set<String>> FIAT_PAYIN =
      Map.ofEntries(
          Map.entry("DETECTED", Set.of("PENDING_SCREENING")),
          Map.entry("PENDING_SCREENING", Set.of("SCREENING_IN_PROGRESS")),
          Map.entry("SCREENING_IN_PROGRESS", Set.of("SCREENING_CLEARED", "SCREENING_FLAGGED")),
          Map.entry("SCREENING_CLEARED", Set.of("MATCHED")),
          Map.entry("SCREENING_FLAGGED", Set.of("FAILED", "SUSPENDED")),
          Map.entry("MATCHED", Set.of("PENDING_ALLOCATION")),
          Map.entry("PENDING_ALLOCATION", Set.of("ALLOCATED")),
          Map.entry("ALLOCATED", Set.of("COMPLETED")),
          Map.entry("COMPLETED", Set.of()),
          Map.entry("FAILED", Set.of()),
          Map.entry("SUSPENDED", Set.of()),
          Map.entry("RETURNED", Set.of()),
          Map.entry("UNMATCHED", Set.of("MATCHED", "RETURNED")));

  private static final Map<String, Set<String>> CRYPTO_PAYIN =
      Map.ofEntries(
          Map.entry("DETECTED", Set.of("PENDING_SCREENING")),
          Map.entry("PENDING_SCREENING", Set.of("SCREENING_IN_PROGRESS")),
          Map.entry("SCREENING_IN_PROGRESS", Set.of("SCREENING_CLEARED", "SCREENING_FLAGGED")),
          Map.entry("SCREENING_CLEARED", Set.of("CONFIRMING")),
          Map.entry("SCREENING_FLAGGED", Set.of("FAILED")),
          Map.entry("CONFIRMING", Set.of("CONFIRMED")),
          Map.entry("CONFIRMED", Set.of("CREDITED")),
          Map.entry("CREDITED", Set.of("COMPLETED")),
          Map.entry("COMPLETED", Set.of()),
          Map.entry("FAILED", Set.of()),
          Map.entry("DROPPED", Set.of()));

  private static final Map<String, Set<String>> CHAIN_TRANSACTION =
      Map.ofEntries(
          Map.entry("PENDING", Set.of("BROADCAST")),
          Map.entry("BROADCAST", Set.of("CONFIRMING", "STUCK")),
          Map.entry("CONFIRMING", Set.of("CONFIRMED")),
          Map.entry("CONFIRMED", Set.of()),
          Map.entry("FAILED", Set.of()),
          Map.entry("DROPPED", Set.of()),
          Map.entry("STUCK", Set.of("RBF_ATTEMPTED")),
          Map.entry("RBF_ATTEMPTED", Set.of("REPLACED", "FAILED")),
          Map.entry("REPLACED", Set.of("CONFIRMING")));

  private static final Map<String, Map<String, Set<String>>> ALL_GRAPHS =
      Map.of(
          "payment.payout.fiat.v1", FIAT_PAYOUT,
          "payment.payout.crypto.v1", CRYPTO_PAYOUT,
          "payment.payin.fiat.v1", FIAT_PAYIN,
          "payment.payin.crypto.v1", CRYPTO_PAYIN,
          "chain.transaction.v1", CHAIN_TRANSACTION);

  public static boolean isValidTransition(String topic, String fromStatus, String toStatus) {
    var graph = ALL_GRAPHS.get(topic);
    if (graph == null) {
      return true;
    }
    var validTargets = graph.get(fromStatus);
    if (validTargets == null) {
      return true;
    }
    return validTargets.contains(toStatus);
  }
}
