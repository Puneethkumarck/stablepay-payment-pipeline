package io.stablepay.flink.sink;

import java.io.Serializable;
import java.util.Map;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.apache.iceberg.types.Types;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FactTableSinkFactory implements Serializable {

    static final Schema FACT_TRANSACTIONS_SCHEMA = new Schema(
            Types.NestedField.required(1, "event_id", Types.StringType.get()),
            Types.NestedField.required(2, "event_time", Types.TimestampType.withZone()),
            Types.NestedField.required(3, "ingest_time", Types.TimestampType.withZone()),
            Types.NestedField.optional(4, "flow_id", Types.StringType.get()),
            Types.NestedField.optional(5, "correlation_id", Types.StringType.get()),
            Types.NestedField.optional(6, "trace_id", Types.StringType.get()),
            Types.NestedField.required(7, "event_type", Types.StringType.get()),
            Types.NestedField.required(8, "flow_type", Types.StringType.get()),
            Types.NestedField.required(9, "direction", Types.StringType.get()),
            Types.NestedField.required(10, "is_crypto", Types.BooleanType.get()),
            Types.NestedField.optional(11, "is_user_facing", Types.BooleanType.get()),
            Types.NestedField.optional(12, "transaction_reference", Types.StringType.get()),
            Types.NestedField.optional(13, "customer_id", Types.StringType.get()),
            Types.NestedField.optional(14, "account_id", Types.StringType.get()),
            Types.NestedField.optional(15, "amount_micros", Types.LongType.get()),
            Types.NestedField.optional(16, "currency_code", Types.StringType.get()),
            Types.NestedField.optional(17, "fee_amount_micros", Types.LongType.get()),
            Types.NestedField.optional(18, "fee_currency_code", Types.StringType.get()),
            Types.NestedField.optional(19, "source_amount_micros", Types.LongType.get()),
            Types.NestedField.optional(20, "source_currency_code", Types.StringType.get()),
            Types.NestedField.optional(21, "target_amount_micros", Types.LongType.get()),
            Types.NestedField.optional(22, "target_currency_code", Types.StringType.get()),
            Types.NestedField.optional(23, "fx_rate", Types.DoubleType.get()),
            Types.NestedField.optional(24, "internal_status", Types.StringType.get()),
            Types.NestedField.optional(25, "customer_status", Types.StringType.get()),
            Types.NestedField.optional(26, "screening_outcome", Types.StringType.get()),
            Types.NestedField.optional(27, "chain", Types.StringType.get()),
            Types.NestedField.optional(28, "asset", Types.StringType.get()),
            Types.NestedField.optional(29, "source_address", Types.StringType.get()),
            Types.NestedField.optional(30, "destination_address", Types.StringType.get()),
            Types.NestedField.optional(31, "tx_hash", Types.StringType.get()),
            Types.NestedField.optional(32, "confirmations", Types.IntegerType.get()),
            Types.NestedField.optional(33, "gas_fee_micros", Types.LongType.get()),
            Types.NestedField.optional(34, "block_number", Types.LongType.get()),
            Types.NestedField.optional(35, "block_timestamp", Types.TimestampType.withZone()),
            Types.NestedField.optional(36, "provider", Types.StringType.get()),
            Types.NestedField.optional(37, "route", Types.StringType.get()),
            Types.NestedField.optional(38, "beneficiary_name", Types.StringType.get()),
            Types.NestedField.optional(39, "sender_name", Types.StringType.get()),
            Types.NestedField.optional(40, "description", Types.StringType.get()),
            Types.NestedField.optional(41, "notes", Types.StringType.get()));

    static final PartitionSpec FACT_TRANSACTIONS_PARTITION_SPEC =
            PartitionSpec.builderFor(FACT_TRANSACTIONS_SCHEMA)
                    .day("event_time")
                    .bucket("customer_id", 16)
                    .build();

    static final Schema FACT_FLOWS_SCHEMA = new Schema(
            Types.NestedField.required(1, "flow_id", Types.StringType.get()),
            Types.NestedField.optional(2, "customer_id", Types.StringType.get()),
            Types.NestedField.optional(3, "flow_type", Types.StringType.get()),
            Types.NestedField.optional(4, "flow_status", Types.StringType.get()),
            Types.NestedField.required(5, "initiated_at", Types.TimestampType.withZone()),
            Types.NestedField.optional(6, "completed_at", Types.TimestampType.withZone()),
            Types.NestedField.optional(7, "last_updated_at", Types.TimestampType.withZone()),
            Types.NestedField.optional(8, "payin_status", Types.StringType.get()),
            Types.NestedField.optional(9, "payin_amount_micros", Types.LongType.get()),
            Types.NestedField.optional(10, "payin_currency", Types.StringType.get()),
            Types.NestedField.optional(11, "payin_reference", Types.StringType.get()),
            Types.NestedField.optional(12, "trade_status", Types.StringType.get()),
            Types.NestedField.optional(13, "trade_amount_micros", Types.LongType.get()),
            Types.NestedField.optional(14, "trade_currency", Types.StringType.get()),
            Types.NestedField.optional(15, "payout_status", Types.StringType.get()),
            Types.NestedField.optional(16, "payout_amount_micros", Types.LongType.get()),
            Types.NestedField.optional(17, "payout_currency", Types.StringType.get()),
            Types.NestedField.optional(18, "payout_reference", Types.StringType.get()),
            Types.NestedField.optional(19, "total_duration_ms", Types.LongType.get()),
            Types.NestedField.optional(20, "leg_count", Types.IntegerType.get()));

    static final PartitionSpec FACT_FLOWS_PARTITION_SPEC =
            PartitionSpec.builderFor(FACT_FLOWS_SCHEMA)
                    .day("initiated_at")
                    .build();

    static final Schema FACT_SCREENING_SCHEMA = new Schema(
            Types.NestedField.required(1, "event_id", Types.StringType.get()),
            Types.NestedField.optional(2, "screening_id", Types.StringType.get()),
            Types.NestedField.optional(3, "customer_id", Types.StringType.get()),
            Types.NestedField.optional(4, "transaction_reference", Types.StringType.get()),
            Types.NestedField.optional(5, "outcome", Types.StringType.get()),
            Types.NestedField.optional(6, "provider", Types.StringType.get()),
            Types.NestedField.optional(7, "rule_triggered", Types.StringType.get()),
            Types.NestedField.optional(8, "score", Types.DoubleType.get()),
            Types.NestedField.required(9, "event_time", Types.TimestampType.withZone()),
            Types.NestedField.optional(10, "duration_ms", Types.LongType.get()));

    static final PartitionSpec FACT_SCREENING_PARTITION_SPEC =
            PartitionSpec.builderFor(FACT_SCREENING_SCHEMA)
                    .day("event_time")
                    .build();

    private static final Map<String, SchemaAndSpec> TABLE_DEFINITIONS = Map.of(
            "fact_transactions", new SchemaAndSpec(FACT_TRANSACTIONS_SCHEMA, FACT_TRANSACTIONS_PARTITION_SPEC),
            "fact_flows", new SchemaAndSpec(FACT_FLOWS_SCHEMA, FACT_FLOWS_PARTITION_SPEC),
            "fact_screening_outcomes", new SchemaAndSpec(FACT_SCREENING_SCHEMA, FACT_SCREENING_PARTITION_SPEC));

    private CatalogLoader createCatalogLoader() {
        return CatalogLoader.custom(
                IcebergCatalogConfig.CATALOG_NAME,
                IcebergCatalogConfig.catalogProperties(),
                new org.apache.hadoop.conf.Configuration(),
                "org.apache.iceberg.jdbc.JdbcCatalog");
    }

    public void ensureFactTablesExist() {
        var catalog = createCatalogLoader().loadCatalog();
        var namespace = Namespace.of(IcebergCatalogConfig.FACTS_NAMESPACE);

        for (var tableName : IcebergCatalogConfig.FACT_TABLES) {
            var tableId = TableIdentifier.of(namespace, tableName);
            if (!catalog.tableExists(tableId)) {
                var def = TABLE_DEFINITIONS.get(tableName);
                catalog.createTable(tableId, def.schema(), def.spec(), Map.of(
                        "format-version", "2",
                        "write.parquet.compression-codec", "zstd"));
                log.info("Created Iceberg fact table: {}", tableId);
            }
        }
    }

    public void addSink(DataStream<RowData> stream, String tableName) {
        var tableId = TableIdentifier.of(
                Namespace.of(IcebergCatalogConfig.FACTS_NAMESPACE), tableName);
        var tableLoader = TableLoader.fromCatalog(createCatalogLoader(), tableId);

        FlinkSink.forRowData(stream)
                .tableLoader(tableLoader)
                .append();
    }

    private record SchemaAndSpec(Schema schema, PartitionSpec spec) {}
}
