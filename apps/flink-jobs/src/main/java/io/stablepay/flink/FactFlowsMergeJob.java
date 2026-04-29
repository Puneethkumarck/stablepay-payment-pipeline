package io.stablepay.flink;

import java.util.Map;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import io.stablepay.flink.sink.FactTableSinkFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FactFlowsMergeJob {

    public static void main(String[] args) throws Exception {
        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        var tableEnv = StreamTableEnvironment.create(env);

        var factSinkFactory = new FactTableSinkFactory();
        factSinkFactory.ensureFactTablesExist();

        registerIcebergCatalog(tableEnv);

        log.info("Executing MERGE INTO facts.fact_flows from raw tables");
        tableEnv.executeSql(buildMergeSql());
        log.info("MERGE INTO facts.fact_flows completed");
    }

    private static void registerIcebergCatalog(StreamTableEnvironment tableEnv) {
        var props = IcebergCatalogConfig.catalogProperties();
        tableEnv.executeSql(String.format(
                "CREATE CATALOG %s WITH ("
                + "'type'='iceberg',"
                + "'catalog-impl'='org.apache.iceberg.jdbc.JdbcCatalog',"
                + "'uri'='%s',"
                + "'jdbc.user'='%s',"
                + "'jdbc.password'='%s',"
                + "'warehouse'='%s',"
                + "'io-impl'='%s',"
                + "'s3.endpoint'='%s',"
                + "'s3.access-key-id'='%s',"
                + "'s3.secret-access-key'='%s',"
                + "'s3.path-style-access'='true'"
                + ")",
                IcebergCatalogConfig.CATALOG_NAME,
                props.get("uri"),
                props.get("jdbc.user"),
                props.get("jdbc.password"),
                props.get("warehouse"),
                props.get("io-impl"),
                props.get("s3.endpoint"),
                props.get("s3.access-key-id"),
                props.get("s3.secret-access-key")));

        tableEnv.executeSql("USE CATALOG " + IcebergCatalogConfig.CATALOG_NAME);
    }

    private static String buildMergeSql() {
        return """
                MERGE INTO facts.fact_flows AS t
                USING (
                    SELECT
                        f.flow_id,
                        f.customer_id,
                        f.flow_type,
                        f.flow_status,
                        f.initiated_at,
                        f.completed_at,
                        f.last_updated_at,
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
                        CASE WHEN f.completed_at IS NOT NULL AND f.initiated_at IS NOT NULL
                             THEN TIMESTAMPDIFF(SECOND, f.initiated_at, f.completed_at) * 1000
                             ELSE NULL END AS total_duration_ms,
                        f.leg_count
                    FROM (
                        SELECT
                            flow_id,
                            LAST_VALUE(customer_id) AS customer_id,
                            LAST_VALUE(flow_type) AS flow_type,
                            LAST_VALUE(flow_status) AS flow_status,
                            MIN(event_time) AS initiated_at,
                            MAX(CASE WHEN flow_status = 'COMPLETED' THEN event_time ELSE NULL END) AS completed_at,
                            MAX(event_time) AS last_updated_at,
                            LAST_VALUE(leg_count) AS leg_count
                        FROM (
                            SELECT
                                JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                                JSON_VALUE(payload_json, '$.customer_id') AS customer_id,
                                JSON_VALUE(payload_json, '$.flow_type') AS flow_type,
                                JSON_VALUE(payload_json, '$.flow_status') AS flow_status,
                                event_time,
                                CAST(JSON_VALUE(payload_json, '$.leg_count') AS INT) AS leg_count
                            FROM raw.raw_payment_flow
                        )
                        GROUP BY flow_id
                    ) f
                    LEFT JOIN (
                        SELECT
                            JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.internal_status')) AS payin_status,
                            LAST_VALUE(CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT)) AS payin_amount_micros,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.amount.currency_code')) AS payin_currency,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.payin_reference')) AS payin_reference
                        FROM raw.raw_payment_payin_fiat
                        GROUP BY JSON_VALUE(payload_json, '$.flow_id')
                    ) pi ON f.flow_id = pi.flow_id
                    LEFT JOIN (
                        SELECT
                            JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.internal_status')) AS trade_status,
                            LAST_VALUE(CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT)) AS trade_amount_micros,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.amount.currency_code')) AS trade_currency
                        FROM raw.raw_chain_transaction
                        GROUP BY JSON_VALUE(payload_json, '$.flow_id')
                    ) tr ON f.flow_id = tr.flow_id
                    LEFT JOIN (
                        SELECT
                            JSON_VALUE(payload_json, '$.flow_id') AS flow_id,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.internal_status')) AS payout_status,
                            LAST_VALUE(CAST(JSON_VALUE(payload_json, '$.amount.amount_micros') AS BIGINT)) AS payout_amount_micros,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.amount.currency_code')) AS payout_currency,
                            LAST_VALUE(JSON_VALUE(payload_json, '$.payout_reference')) AS payout_reference
                        FROM raw.raw_payment_payout_fiat
                        GROUP BY JSON_VALUE(payload_json, '$.flow_id')
                    ) po ON f.flow_id = po.flow_id
                ) AS s
                ON t.flow_id = s.flow_id
                WHEN MATCHED THEN UPDATE SET
                    customer_id = s.customer_id,
                    flow_type = s.flow_type,
                    flow_status = s.flow_status,
                    initiated_at = s.initiated_at,
                    completed_at = s.completed_at,
                    last_updated_at = s.last_updated_at,
                    payin_status = s.payin_status,
                    payin_amount_micros = s.payin_amount_micros,
                    payin_currency = s.payin_currency,
                    payin_reference = s.payin_reference,
                    trade_status = s.trade_status,
                    trade_amount_micros = s.trade_amount_micros,
                    trade_currency = s.trade_currency,
                    payout_status = s.payout_status,
                    payout_amount_micros = s.payout_amount_micros,
                    payout_currency = s.payout_currency,
                    payout_reference = s.payout_reference,
                    total_duration_ms = s.total_duration_ms,
                    leg_count = s.leg_count
                WHEN NOT MATCHED THEN INSERT (
                    flow_id, customer_id, flow_type, flow_status,
                    initiated_at, completed_at, last_updated_at,
                    payin_status, payin_amount_micros, payin_currency, payin_reference,
                    trade_status, trade_amount_micros, trade_currency,
                    payout_status, payout_amount_micros, payout_currency, payout_reference,
                    total_duration_ms, leg_count
                ) VALUES (
                    s.flow_id, s.customer_id, s.flow_type, s.flow_status,
                    s.initiated_at, s.completed_at, s.last_updated_at,
                    s.payin_status, s.payin_amount_micros, s.payin_currency, s.payin_reference,
                    s.trade_status, s.trade_amount_micros, s.trade_currency,
                    s.payout_status, s.payout_amount_micros, s.payout_currency, s.payout_reference,
                    s.total_duration_ms, s.leg_count
                )
                """;
    }
}
