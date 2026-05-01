package io.stablepay.flink;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import io.stablepay.flink.sink.FactTableSinkFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

@Slf4j
public class FactFlowsMergeJob {

  public static void main(String[] args) throws Exception {
    var env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setRuntimeMode(RuntimeExecutionMode.BATCH);
    var tableEnv = StreamTableEnvironment.create(env);

    var factSinkFactory = new FactTableSinkFactory();
    factSinkFactory.ensureFactTablesExist();

    registerIcebergCatalog(tableEnv);

    log.info("Executing INSERT OVERWRITE facts.fact_flows from raw tables");
    tableEnv.executeSql(buildInsertOverwriteSql());
    log.info("INSERT OVERWRITE facts.fact_flows completed");
  }

  private static void registerIcebergCatalog(StreamTableEnvironment tableEnv) {
    var props = IcebergCatalogConfig.catalogProperties();
    var sql =
        "CREATE CATALOG "
            + IcebergCatalogConfig.CATALOG_NAME
            + " WITH ("
            + "'type'='iceberg',"
            + "'catalog-impl'='org.apache.iceberg.jdbc.JdbcCatalog',"
            + "'uri'='"
            + escapeSql(props.get("uri"))
            + "',"
            + "'jdbc.user'='"
            + escapeSql(props.get("jdbc.user"))
            + "',"
            + "'jdbc.password'='"
            + escapeSql(props.get("jdbc.password"))
            + "',"
            + "'warehouse'='"
            + escapeSql(props.get("warehouse"))
            + "',"
            + "'io-impl'='"
            + escapeSql(props.get("io-impl"))
            + "',"
            + "'s3.endpoint'='"
            + escapeSql(props.get("s3.endpoint"))
            + "',"
            + "'s3.access-key-id'='"
            + escapeSql(props.get("s3.access-key-id"))
            + "',"
            + "'s3.secret-access-key'='"
            + escapeSql(props.get("s3.secret-access-key"))
            + "',"
            + "'s3.path-style-access'='true'"
            + ")";

    tableEnv.executeSql(sql);
    tableEnv.executeSql("USE CATALOG " + IcebergCatalogConfig.CATALOG_NAME);
  }

  static String escapeSql(String value) {
    return value != null ? value.replace("'", "''") : "";
  }

  static String buildInsertOverwriteSql() {
    return """
                INSERT OVERWRITE facts.fact_flows
                SELECT
                    f.flow_id,
                    f.customer_id,
                    f.flow_type,
                    f.flow_status,
                    aggs.initiated_at,
                    aggs.completed_at,
                    aggs.last_updated_at,
                    pi.payin_status,
                    pi.payin_amount_micros,
                    pi.payin_currency,
                    pi.payin_reference,
                    tr.trade_status,
                    tr.trade_amount_micros,
                    tr.trade_currency,
                    po.payout_status,
                    po.payout_amount_micros,
                    po.payout_currency,
                    po.payout_reference,
                    CASE WHEN aggs.completed_at IS NOT NULL
                         THEN TIMESTAMPDIFF(SECOND, aggs.initiated_at, aggs.completed_at) * 1000
                         ELSE NULL END,
                    f.leg_count
                FROM (
                    SELECT flow_id, customer_id, flow_type, flow_status, leg_count
                    FROM (
                        SELECT
                            JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                            JSON_VALUE(payload_json, '$.customer_id') AS customer_id,
                            JSON_VALUE(payload_json, '$.flow_type') AS flow_type,
                            JSON_VALUE(payload_json, '$.flow_status') AS flow_status,
                            CAST(JSON_VALUE(payload_json, '$.leg_count') AS INT) AS leg_count,
                            ROW_NUMBER() OVER (
                                PARTITION BY JSON_VALUE(payload_json, '$.flow_id')
                                ORDER BY event_time DESC
                            ) AS rn
                        FROM raw.raw_payment_flow
                    ) WHERE rn = 1
                ) f
                JOIN (
                    SELECT
                        JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                        MIN(event_time) AS initiated_at,
                        MAX(CASE WHEN JSON_VALUE(payload_json, '$.flow_status') = 'COMPLETED'
                                 THEN event_time END) AS completed_at,
                        MAX(event_time) AS last_updated_at
                    FROM raw.raw_payment_flow
                    GROUP BY JSON_VALUE(payload_json, '$.flow_id')
                ) aggs ON f.flow_id = aggs.flow_id
                LEFT JOIN (
                    SELECT flow_id, payin_status, payin_amount_micros, payin_currency, payin_reference
                    FROM (
                        SELECT
                            flow_id,
                            internal_status AS payin_status,
                            amount_micros AS payin_amount_micros,
                            currency_code AS payin_currency,
                            payin_reference,
                            ROW_NUMBER() OVER (PARTITION BY flow_id ORDER BY event_time DESC) AS rn
                        FROM (
                            SELECT
                                JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                                JSON_VALUE(payload_json, '$.internal_status') AS internal_status,
                                CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT) AS amount_micros,
                                JSON_VALUE(payload_json, '$.amount.currency_code') AS currency_code,
                                JSON_VALUE(payload_json, '$.payin_reference') AS payin_reference,
                                event_time
                            FROM raw.raw_payment_payin_fiat
                            UNION ALL
                            SELECT
                                JSON_VALUE(payload_json, '$.flow_id'),
                                JSON_VALUE(payload_json, '$.internal_status'),
                                CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT),
                                JSON_VALUE(payload_json, '$.amount.currency_code'),
                                JSON_VALUE(payload_json, '$.payin_reference'),
                                event_time
                            FROM raw.raw_payment_payin_crypto
                        )
                    ) WHERE rn = 1
                ) pi ON f.flow_id = pi.flow_id
                LEFT JOIN (
                    SELECT flow_id, trade_status, trade_amount_micros, trade_currency
                    FROM (
                        SELECT
                            JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                            JSON_VALUE(payload_json, '$.internal_status') AS trade_status,
                            CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT) AS trade_amount_micros,
                            JSON_VALUE(payload_json, '$.amount.currency_code') AS trade_currency,
                            ROW_NUMBER() OVER (
                                PARTITION BY JSON_VALUE(payload_json, '$.flow_id')
                                ORDER BY event_time DESC
                            ) AS rn
                        FROM raw.raw_chain_transaction
                    ) WHERE rn = 1
                ) tr ON f.flow_id = tr.flow_id
                LEFT JOIN (
                    SELECT flow_id, payout_status, payout_amount_micros, payout_currency, payout_reference
                    FROM (
                        SELECT
                            flow_id,
                            internal_status AS payout_status,
                            amount_micros AS payout_amount_micros,
                            currency_code AS payout_currency,
                            payout_reference,
                            ROW_NUMBER() OVER (PARTITION BY flow_id ORDER BY event_time DESC) AS rn
                        FROM (
                            SELECT
                                JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                                JSON_VALUE(payload_json, '$.internal_status') AS internal_status,
                                CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT) AS amount_micros,
                                JSON_VALUE(payload_json, '$.amount.currency_code') AS currency_code,
                                JSON_VALUE(payload_json, '$.payout_reference') AS payout_reference,
                                event_time
                            FROM raw.raw_payment_payout_fiat
                            UNION ALL
                            SELECT
                                JSON_VALUE(payload_json, '$.flow_id'),
                                JSON_VALUE(payload_json, '$.internal_status'),
                                CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT),
                                JSON_VALUE(payload_json, '$.amount.currency_code'),
                                JSON_VALUE(payload_json, '$.payout_reference'),
                                event_time
                            FROM raw.raw_payment_payout_crypto
                        )
                    ) WHERE rn = 1
                ) po ON f.flow_id = po.flow_id
                """;
  }
}
