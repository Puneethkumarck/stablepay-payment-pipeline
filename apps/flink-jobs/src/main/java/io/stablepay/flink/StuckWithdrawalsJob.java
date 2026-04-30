package io.stablepay.flink;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

import io.stablepay.flink.catalog.IcebergCatalogConfig;

public class StuckWithdrawalsJob {

    public static void main(String[] args) {
        var config = new Configuration();
        config.set(PipelineOptions.NAME, "stablepay-stuck-withdrawals-job");
        var settings = EnvironmentSettings.newInstance().inBatchMode().withConfiguration(config).build();
        var tableEnv = TableEnvironment.create(settings);

        var catalogProps = IcebergCatalogConfig.catalogProperties();
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
                catalogProps.get("uri"),
                catalogProps.get("jdbc.user"),
                catalogProps.get("jdbc.password"),
                catalogProps.get("warehouse"),
                catalogProps.get("io-impl"),
                catalogProps.get("s3.endpoint"),
                catalogProps.get("s3.access-key-id"),
                catalogProps.get("s3.secret-access-key")));

        tableEnv.useCatalog(IcebergCatalogConfig.CATALOG_NAME);

        tableEnv.executeSql(
                "INSERT OVERWRITE " + IcebergCatalogConfig.AGG_NAMESPACE + ".agg_stuck_withdrawals "
                + "SELECT "
                + "  CURRENT_TIMESTAMP AS snapshot_time, "
                + "  transaction_reference, customer_id, "
                + "  amount_micros, currency_code, internal_status, "
                + "  event_time, "
                + "  TIMESTAMPDIFF(MILLISECOND, event_time, CURRENT_TIMESTAMP) AS stuck_duration_ms, "
                + "  chain, asset "
                + "FROM facts.fact_transactions "
                + "WHERE is_crypto = true "
                + "  AND direction = 'OUTBOUND' "
                + "  AND internal_status NOT IN ("
                + "    'COMPLETED','FAILED','CANCELLED','REJECTED','REFUNDED','CONFISCATED') "
                + "  AND event_time < CURRENT_TIMESTAMP - INTERVAL '1' HOUR");
    }
}
