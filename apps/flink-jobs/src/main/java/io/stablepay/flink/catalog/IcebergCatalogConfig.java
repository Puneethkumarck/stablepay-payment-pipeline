package io.stablepay.flink.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IcebergCatalogConfig {

  private IcebergCatalogConfig() {}

  public static final String CATALOG_NAME = "iceberg_catalog";
  public static final String RAW_NAMESPACE = "raw";
  public static final String AGG_NAMESPACE = "agg";
  public static final String FACTS_NAMESPACE = "facts";
  public static final String DLQ_NAMESPACE = "dlq";

  public static final List<String> AGG_TABLES =
      List.of(
          "agg_volume_hourly",
          "agg_success_rate_hourly",
          "agg_screening_outcomes_daily",
          "agg_dlq_summary_hourly",
          "agg_stuck_withdrawals");

  public static final List<String> FACT_TABLES =
      List.of("fact_transactions", "fact_flows", "fact_screening_outcomes");

  public static final List<String> RAW_TABLES =
      List.of(
          "raw_payment_payout_fiat",
          "raw_payment_payout_crypto",
          "raw_payment_payin_fiat",
          "raw_payment_payin_crypto",
          "raw_payment_flow",
          "raw_chain_transaction",
          "raw_signing_request",
          "raw_screening_result",
          "raw_approval_decision");

  public static final Map<String, String> TOPIC_TO_TABLE =
      Map.of(
          "payment.payout.fiat.v1", "raw_payment_payout_fiat",
          "payment.payout.crypto.v1", "raw_payment_payout_crypto",
          "payment.payin.fiat.v1", "raw_payment_payin_fiat",
          "payment.payin.crypto.v1", "raw_payment_payin_crypto",
          "payment.flow.v1", "raw_payment_flow",
          "chain.transaction.v1", "raw_chain_transaction",
          "signing.request.v1", "raw_signing_request",
          "screening.result.v1", "raw_screening_result",
          "approval.decision.v1", "raw_approval_decision");

  public static Map<String, String> catalogProperties() {
    var props = new HashMap<String, String>();
    props.put("type", "jdbc");
    props.put(
        "uri",
        System.getenv()
            .getOrDefault(
                "STBLPAY_ICEBERG_JDBC_URI", "jdbc:postgresql://postgres:5432/iceberg_catalog"));
    props.put("jdbc.user", System.getenv().getOrDefault("POSTGRES_USER", "stablepay"));
    props.put("jdbc.password", System.getenv().getOrDefault("POSTGRES_PASSWORD", "stablepay_dev"));
    props.put("warehouse", "s3a://warehouse/");
    props.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
    props.put(
        "s3.endpoint", System.getenv().getOrDefault("STBLPAY_S3_ENDPOINT", "http://minio:9000"));
    props.put("s3.access-key-id", System.getenv().getOrDefault("MINIO_ROOT_USER", "minioadmin"));
    props.put(
        "s3.secret-access-key",
        System.getenv().getOrDefault("MINIO_ROOT_PASSWORD", "minioadmin123"));
    props.put("s3.path-style-access", "true");
    return Map.copyOf(props);
  }
}
