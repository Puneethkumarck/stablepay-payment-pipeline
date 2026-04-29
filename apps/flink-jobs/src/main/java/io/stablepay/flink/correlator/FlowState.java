package io.stablepay.flink.correlator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowState implements Serializable {

    private String flowId;
    private String customerId;
    private String flowType;
    private String currentFlowStatus;
    private Map<String, LegState> legs;
    private long initiatedAt;
    private long lastUpdatedAt;

    public FlowState() {
        this.legs = new HashMap<>();
    }

    public record LegState(
            String legId,
            String legType,
            String status,
            String reference) implements Serializable {}

    public void initialize(String flowId, String customerId, String flowType, long initiatedAt) {
        this.flowId = flowId;
        this.customerId = customerId;
        this.flowType = flowType;
        this.currentFlowStatus = "INITIATED";
        this.initiatedAt = initiatedAt;
        this.lastUpdatedAt = initiatedAt;
    }

    public void updateLeg(String legId, String legType, String status, String reference) {
        legs.put(legId, new LegState(legId, legType, status, reference));
        lastUpdatedAt = System.currentTimeMillis();
    }

    public boolean allLegsCompleted() {
        if (legs.size() < 3) return false;
        return legs.values().stream().allMatch(l -> "COMPLETED".equals(l.status()));
    }

    public boolean anyLegFailed() {
        return legs.values().stream().anyMatch(l -> "FAILED".equals(l.status()) || "CANCELLED".equals(l.status()));
    }

    public boolean anyLegCompleted() {
        return legs.values().stream().anyMatch(l -> "COMPLETED".equals(l.status()));
    }

    public String deriveFlowStatus() {
        if (allLegsCompleted()) return "COMPLETED";

        if (anyLegCompleted() && anyLegFailed()) {
            if ("COMPENSATION_INITIATED".equals(currentFlowStatus)
                    || "COMPENSATION_COMPLETED".equals(currentFlowStatus)) {
                return currentFlowStatus;
            }
            return "PARTIALLY_COMPLETED";
        }

        if (legs.size() == 3 && legs.values().stream().allMatch(
                l -> "FAILED".equals(l.status()) || "CANCELLED".equals(l.status()))) {
            return "FAILED";
        }

        boolean hasPayinCompleted = legs.values().stream()
                .anyMatch(l -> "PAYIN".equals(l.legType()) && "COMPLETED".equals(l.status()));
        boolean hasPayoutStarted = legs.values().stream()
                .anyMatch(l -> "PAYOUT".equals(l.legType()));
        boolean hasTradeCompleted = legs.values().stream()
                .anyMatch(l -> "TRADE".equals(l.legType()) && "COMPLETED".equals(l.status()));
        boolean hasTradeStarted = legs.values().stream()
                .anyMatch(l -> "TRADE".equals(l.legType()));
        boolean hasPayinStarted = legs.values().stream()
                .anyMatch(l -> "PAYIN".equals(l.legType()));

        if (hasPayoutStarted && hasTradeCompleted) return "PAYOUT_PENDING";
        if (hasTradeCompleted) return "TRADE_COMPLETED";
        if (hasTradeStarted && hasPayinCompleted) return "TRADE_PENDING";
        if (hasPayinCompleted) return "PAYIN_COMPLETED";
        if (hasPayinStarted) return "PAYIN_PENDING";

        return currentFlowStatus != null ? currentFlowStatus : "INITIATED";
    }

    public void setCurrentFlowStatus(String status) {
        this.currentFlowStatus = status;
        this.lastUpdatedAt = System.currentTimeMillis();
    }

    public List<LegState> toLegList() {
        return new ArrayList<>(legs.values());
    }

    public String flowId() { return flowId; }
    public String customerId() { return customerId; }
    public String flowType() { return flowType; }
    public String currentFlowStatus() { return currentFlowStatus; }
    public Map<String, LegState> legs() { return Collections.unmodifiableMap(legs); }
    public long initiatedAt() { return initiatedAt; }
    public long lastUpdatedAt() { return lastUpdatedAt; }
}
