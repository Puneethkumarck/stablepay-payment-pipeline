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
public class IcebergSinkFactory implements Serializable {

    private static final Schema COMMON_SCHEMA = new Schema(
            Types.NestedField.required(1, "event_id", Types.StringType.get()),
            Types.NestedField.required(2, "event_time", Types.TimestampType.withZone()),
            Types.NestedField.required(3, "ingest_time", Types.TimestampType.withZone()),
            Types.NestedField.optional(4, "schema_version", Types.StringType.get()),
            Types.NestedField.optional(5, "flow_id", Types.StringType.get()),
            Types.NestedField.optional(6, "correlation_id", Types.StringType.get()),
            Types.NestedField.optional(7, "trace_id", Types.StringType.get()),
            Types.NestedField.required(8, "topic", Types.StringType.get()),
            Types.NestedField.optional(9, "key", Types.StringType.get()),
            Types.NestedField.required(10, "payload_json", Types.StringType.get()));

    private static final PartitionSpec DEFAULT_PARTITION_SPEC = PartitionSpec.builderFor(COMMON_SCHEMA)
            .day("event_time")
            .build();

    private static final Schema CHAIN_TX_SCHEMA = new Schema(
            Types.NestedField.required(1, "event_id", Types.StringType.get()),
            Types.NestedField.required(2, "event_time", Types.TimestampType.withZone()),
            Types.NestedField.required(3, "ingest_time", Types.TimestampType.withZone()),
            Types.NestedField.optional(4, "schema_version", Types.StringType.get()),
            Types.NestedField.optional(5, "flow_id", Types.StringType.get()),
            Types.NestedField.optional(6, "correlation_id", Types.StringType.get()),
            Types.NestedField.optional(7, "trace_id", Types.StringType.get()),
            Types.NestedField.required(8, "topic", Types.StringType.get()),
            Types.NestedField.optional(9, "key", Types.StringType.get()),
            Types.NestedField.required(10, "payload_json", Types.StringType.get()),
            Types.NestedField.required(11, "tx_hash", Types.StringType.get()));

    private static final PartitionSpec CHAIN_TX_PARTITION_SPEC = PartitionSpec.builderFor(CHAIN_TX_SCHEMA)
            .day("event_time")
            .bucket("tx_hash", 8)
            .build();

    private CatalogLoader createCatalogLoader() {
        return CatalogLoader.custom(
                IcebergCatalogConfig.CATALOG_NAME,
                IcebergCatalogConfig.catalogProperties(),
                new org.apache.hadoop.conf.Configuration(),
                "org.apache.iceberg.jdbc.JdbcCatalog");
    }

    public void ensureTablesExist() {
        var catalog = createCatalogLoader().loadCatalog();
        Namespace namespace = Namespace.of(IcebergCatalogConfig.RAW_NAMESPACE);

        for (String tableName : IcebergCatalogConfig.RAW_TABLES) {
            TableIdentifier tableId = TableIdentifier.of(namespace, tableName);
            if (!catalog.tableExists(tableId)) {
                boolean isChainTx = "raw_chain_transaction".equals(tableName);
                Schema schema = isChainTx ? CHAIN_TX_SCHEMA : COMMON_SCHEMA;
                PartitionSpec spec = isChainTx ? CHAIN_TX_PARTITION_SPEC : DEFAULT_PARTITION_SPEC;

                catalog.createTable(tableId, schema, spec, Map.of(
                        "format-version", "2",
                        "write.parquet.compression-codec", "zstd"));
                log.info("Created Iceberg table: {}", tableId);
            }
        }
    }

    public void addSink(DataStream<RowData> stream, String tableName) {
        TableIdentifier tableId = TableIdentifier.of(
                Namespace.of(IcebergCatalogConfig.RAW_NAMESPACE), tableName);
        TableLoader tableLoader = TableLoader.fromCatalog(createCatalogLoader(), tableId);

        FlinkSink.forRowData(stream)
                .tableLoader(tableLoader)
                .append();
    }
}
