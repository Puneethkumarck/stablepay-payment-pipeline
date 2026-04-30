package io.stablepay.flink;

import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StuckWithdrawalsJob {

    private static final String JOB_NAME = "stablepay-stuck-withdrawals-job";

    public static void main(String[] args) throws Exception {
        log.info("Starting {}", JOB_NAME);
        var settings = EnvironmentSettings.newInstance().inBatchMode().build();
        var tableEnv = TableEnvironment.create(settings);
        tableEnv.getConfig().set(PipelineOptions.NAME, JOB_NAME);

        var catalogProps = IcebergCatalogConfig.catalogProperties();
        tableEnv.executeSql(String.format(
                "CREATE CATALOG IF NOT EXISTS %s WITH ("
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
                sqlLiteral(catalogProps.get("uri")),
                sqlLiteral(catalogProps.get("jdbc.user")),
                sqlLiteral(catalogProps.get("jdbc.password")),
                sqlLiteral(catalogProps.get("warehouse")),
                sqlLiteral(catalogProps.get("io-impl")),
                sqlLiteral(catalogProps.get("s3.endpoint")),
                sqlLiteral(catalogProps.get("s3.access-key-id")),
                sqlLiteral(catalogProps.get("s3.secret-access-key"))));

        tableEnv.useCatalog(IcebergCatalogConfig.CATALOG_NAME);

        var insertResult = tableEnv.executeSql(
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

        insertResult.await();
        log.info("{} completed successfully", JOB_NAME);
    }

    private static String sqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
