package io.stablepay.flink.topic;

public final class TopicDerivation {

    private TopicDerivation() {}

    public static String deriveFlowType(String topic) {
        if (topic.contains("crypto") || topic.contains("chain")) return "CRYPTO";
        if (topic.contains("fiat")) return "FIAT";
        return "MIXED";
    }

    public static String deriveDirection(String topic) {
        if (topic.contains("payin")) return "INBOUND";
        if (topic.contains("payout")) return "OUTBOUND";
        return "INTERNAL";
    }
}
