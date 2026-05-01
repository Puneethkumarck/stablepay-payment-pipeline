package io.stablepay.flink.sink;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import java.io.Serializable;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.apache.iceberg.types.Types;

@Slf4j
public class DlqIcebergSinkFactory implements Serializable {

  private static final long serialVersionUID = 1L;

  static final Schema DLQ_EVENTS_SCHEMA =
      new Schema(
          Types.NestedField.required(1, "event_id", Types.StringType.get()),
          Types.NestedField.required(2, "source_topic", Types.StringType.get()),
          Types.NestedField.required(3, "source_partition", Types.IntegerType.get()),
          Types.NestedField.required(4, "source_offset", Types.LongType.get()),
          Types.NestedField.required(5, "error_class", Types.StringType.get()),
          Types.NestedField.required(6, "error_message", Types.StringType.get()),
          Types.NestedField.required(7, "failed_at", Types.TimestampType.withZone()),
          Types.NestedField.required(8, "retry_count", Types.IntegerType.get()),
          Types.NestedField.optional(9, "original_payload_json", Types.StringType.get()));

  static final PartitionSpec DLQ_EVENTS_PARTITION_SPEC =
      PartitionSpec.builderFor(DLQ_EVENTS_SCHEMA).day("failed_at").bucket("error_class", 4).build();

  private CatalogLoader createCatalogLoader() {
    return CatalogLoader.custom(
        IcebergCatalogConfig.CATALOG_NAME,
        IcebergCatalogConfig.catalogProperties(),
        new org.apache.hadoop.conf.Configuration(),
        "org.apache.iceberg.jdbc.JdbcCatalog");
  }

  public void ensureDlqTableExists() {
    var catalog = createCatalogLoader().loadCatalog();
    var namespace = Namespace.of(IcebergCatalogConfig.DLQ_NAMESPACE);

    if (catalog instanceof SupportsNamespaces nsCatalog && !nsCatalog.namespaceExists(namespace)) {
      nsCatalog.createNamespace(namespace);
      log.info("Created Iceberg namespace: {}", namespace);
    }

    var tableId = TableIdentifier.of(namespace, "dlq_events");
    if (!catalog.tableExists(tableId)) {
      catalog.createTable(
          tableId,
          DLQ_EVENTS_SCHEMA,
          DLQ_EVENTS_PARTITION_SPEC,
          Map.of(
              "format-version", "2",
              "write.parquet.compression-codec", "zstd"));
      log.info("Created Iceberg DLQ table: {}", tableId);
    }
  }

  public void addSink(DataStream<RowData> stream, String tableName) {
    var tableId = TableIdentifier.of(Namespace.of(IcebergCatalogConfig.DLQ_NAMESPACE), tableName);
    var tableLoader = TableLoader.fromCatalog(createCatalogLoader(), tableId);

    FlinkSink.forRowData(stream).tableLoader(tableLoader).append();
  }
}
