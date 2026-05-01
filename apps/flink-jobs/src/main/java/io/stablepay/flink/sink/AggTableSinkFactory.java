package io.stablepay.flink.sink;

import io.stablepay.flink.catalog.IcebergCatalogConfig;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class AggTableSinkFactory implements Serializable {

  static final Schema VOLUME_HOURLY_SCHEMA =
      new Schema(
          List.of(
              Types.NestedField.required(1, "window_start", Types.TimestampType.withZone()),
              Types.NestedField.required(2, "window_end", Types.TimestampType.withZone()),
              Types.NestedField.required(3, "flow_type", Types.StringType.get()),
              Types.NestedField.required(4, "direction", Types.StringType.get()),
              Types.NestedField.required(5, "currency_code", Types.StringType.get()),
              Types.NestedField.required(6, "total_amount_micros", Types.LongType.get()),
              Types.NestedField.required(7, "transaction_count", Types.LongType.get())),
          Set.of(1, 3, 4, 5));

  static final List<String> VOLUME_HOURLY_EQUALITY =
      List.of("window_start", "flow_type", "direction", "currency_code");

  static final Schema SUCCESS_RATE_HOURLY_SCHEMA =
      new Schema(
          List.of(
              Types.NestedField.required(1, "window_start", Types.TimestampType.withZone()),
              Types.NestedField.required(2, "window_end", Types.TimestampType.withZone()),
              Types.NestedField.required(3, "flow_type", Types.StringType.get()),
              Types.NestedField.required(4, "total_count", Types.LongType.get()),
              Types.NestedField.required(5, "completed_count", Types.LongType.get()),
              Types.NestedField.required(6, "failed_count", Types.LongType.get()),
              Types.NestedField.required(7, "success_rate", Types.DoubleType.get())),
          Set.of(1, 3));

  static final List<String> SUCCESS_RATE_HOURLY_EQUALITY = List.of("window_start", "flow_type");

  static final Schema SCREENING_OUTCOMES_DAILY_SCHEMA =
      new Schema(
          List.of(
              Types.NestedField.required(1, "window_date", Types.DateType.get()),
              Types.NestedField.required(2, "outcome", Types.StringType.get()),
              Types.NestedField.required(3, "provider", Types.StringType.get()),
              Types.NestedField.required(4, "total_count", Types.LongType.get()),
              Types.NestedField.required(5, "avg_duration_ms", Types.DoubleType.get()),
              Types.NestedField.required(6, "avg_score", Types.DoubleType.get())),
          Set.of(1, 2, 3));

  static final List<String> SCREENING_OUTCOMES_DAILY_EQUALITY =
      List.of("window_date", "outcome", "provider");

  static final Schema DLQ_SUMMARY_HOURLY_SCHEMA =
      new Schema(
          List.of(
              Types.NestedField.required(1, "window_start", Types.TimestampType.withZone()),
              Types.NestedField.required(2, "window_end", Types.TimestampType.withZone()),
              Types.NestedField.required(3, "error_class", Types.StringType.get()),
              Types.NestedField.required(4, "source_topic", Types.StringType.get()),
              Types.NestedField.required(5, "event_count", Types.LongType.get()),
              Types.NestedField.required(6, "max_retry_count", Types.IntegerType.get())),
          Set.of(1, 3, 4));

  static final List<String> DLQ_SUMMARY_HOURLY_EQUALITY =
      List.of("window_start", "error_class", "source_topic");

  static final Schema STUCK_WITHDRAWALS_SCHEMA =
      new Schema(
          Types.NestedField.required(1, "snapshot_time", Types.TimestampType.withZone()),
          Types.NestedField.required(2, "transaction_reference", Types.StringType.get()),
          Types.NestedField.required(3, "customer_id", Types.StringType.get()),
          Types.NestedField.required(4, "amount_micros", Types.LongType.get()),
          Types.NestedField.required(5, "currency_code", Types.StringType.get()),
          Types.NestedField.required(6, "internal_status", Types.StringType.get()),
          Types.NestedField.required(7, "event_time", Types.TimestampType.withZone()),
          Types.NestedField.required(8, "stuck_duration_ms", Types.LongType.get()),
          Types.NestedField.optional(9, "chain", Types.StringType.get()),
          Types.NestedField.optional(10, "asset", Types.StringType.get()));

  private static final Map<String, SchemaAndSpec> TABLE_CONFIGS =
      Map.of(
          "agg_volume_hourly",
              new SchemaAndSpec(
                  VOLUME_HOURLY_SCHEMA,
                  PartitionSpec.builderFor(VOLUME_HOURLY_SCHEMA).day("window_start").build(),
                  VOLUME_HOURLY_EQUALITY),
          "agg_success_rate_hourly",
              new SchemaAndSpec(
                  SUCCESS_RATE_HOURLY_SCHEMA,
                  PartitionSpec.builderFor(SUCCESS_RATE_HOURLY_SCHEMA).day("window_start").build(),
                  SUCCESS_RATE_HOURLY_EQUALITY),
          "agg_screening_outcomes_daily",
              new SchemaAndSpec(
                  SCREENING_OUTCOMES_DAILY_SCHEMA,
                  PartitionSpec.builderFor(SCREENING_OUTCOMES_DAILY_SCHEMA)
                      .day("window_date")
                      .build(),
                  SCREENING_OUTCOMES_DAILY_EQUALITY),
          "agg_dlq_summary_hourly",
              new SchemaAndSpec(
                  DLQ_SUMMARY_HOURLY_SCHEMA,
                  PartitionSpec.builderFor(DLQ_SUMMARY_HOURLY_SCHEMA).day("window_start").build(),
                  DLQ_SUMMARY_HOURLY_EQUALITY),
          "agg_stuck_withdrawals",
              new SchemaAndSpec(
                  STUCK_WITHDRAWALS_SCHEMA, PartitionSpec.unpartitioned(), List.of()));

  private CatalogLoader createCatalogLoader() {
    return CatalogLoader.custom(
        IcebergCatalogConfig.CATALOG_NAME,
        IcebergCatalogConfig.catalogProperties(),
        new org.apache.hadoop.conf.Configuration(),
        "org.apache.iceberg.jdbc.JdbcCatalog");
  }

  public void ensureAggTablesExist() {
    var catalog = createCatalogLoader().loadCatalog();
    var namespace = Namespace.of(IcebergCatalogConfig.AGG_NAMESPACE);

    for (var tableName : IcebergCatalogConfig.AGG_TABLES) {
      var tableId = TableIdentifier.of(namespace, tableName);
      if (!catalog.tableExists(tableId)) {
        var config = TABLE_CONFIGS.get(tableName);
        catalog.createTable(
            tableId,
            config.schema(),
            config.spec(),
            Map.of(
                "format-version", "2",
                "write.parquet.compression-codec", "zstd"));
        log.info("Created Iceberg agg table: {}", tableId);
      }
    }
  }

  public void addSink(DataStream<RowData> stream, String tableName) {
    var tableId = TableIdentifier.of(Namespace.of(IcebergCatalogConfig.AGG_NAMESPACE), tableName);
    var tableLoader = TableLoader.fromCatalog(createCatalogLoader(), tableId);
    var equalityFields = TABLE_CONFIGS.get(tableName).equalityFields();

    var builder = FlinkSink.forRowData(stream).tableLoader(tableLoader);
    if (!equalityFields.isEmpty()) {
      builder.upsert(true).equalityFieldColumns(equalityFields);
    }
    builder.append();
  }

  private record SchemaAndSpec(Schema schema, PartitionSpec spec, List<String> equalityFields) {}
}
