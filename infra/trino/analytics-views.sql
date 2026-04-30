-- Prerequisites: iceberg.facts.* and iceberg.agg.* tables must exist first.
-- They are created by the Flink streaming jobs in plans 3.1 and 3.2.
-- Running `just trino-init` before those jobs have produced tables will fail
-- with "Table not found" — this is expected on a fresh stack.

CREATE SCHEMA IF NOT EXISTS iceberg.analytics;

-- v_transactions excludes PII columns (beneficiary_name, sender_name) per the
-- masking constraint. Add new fact_transactions columns here explicitly when
-- they ship — SELECT * would silently re-expose PII.
CREATE OR REPLACE VIEW iceberg.analytics.v_transactions AS
SELECT
  event_id, event_time, ingest_time,
  flow_id, correlation_id, trace_id,
  event_type, flow_type, direction, is_crypto, is_user_facing,
  transaction_reference, customer_id, account_id,
  amount_micros, currency_code,
  fee_amount_micros, fee_currency_code,
  source_amount_micros, source_currency_code,
  target_amount_micros, target_currency_code,
  fx_rate,
  internal_status, customer_status, screening_outcome,
  chain, asset, source_address, destination_address, tx_hash,
  confirmations, gas_fee_micros, block_number, block_timestamp,
  provider, route,
  description, notes,
  CASE WHEN internal_status IN ('COMPLETED') THEN 'SUCCESS'
       WHEN internal_status IN ('FAILED','CANCELLED','REJECTED','CONFISCATED') THEN 'FAILED'
       ELSE 'PENDING' END AS status_bucket
FROM iceberg.facts.fact_transactions;

CREATE OR REPLACE VIEW iceberg.analytics.v_flows AS
SELECT *,
  CASE WHEN total_duration_ms < 60000 THEN 'FAST'
       WHEN total_duration_ms < 300000 THEN 'NORMAL'
       ELSE 'SLOW' END AS duration_bucket
FROM iceberg.facts.fact_flows;

CREATE OR REPLACE VIEW iceberg.analytics.v_volume_hourly AS
SELECT * FROM iceberg.agg.agg_volume_hourly;

CREATE OR REPLACE VIEW iceberg.analytics.v_success_rate AS
SELECT * FROM iceberg.agg.agg_success_rate_hourly;

CREATE OR REPLACE VIEW iceberg.analytics.v_stuck_withdrawals AS
SELECT * FROM iceberg.agg.agg_stuck_withdrawals;

CREATE OR REPLACE VIEW iceberg.analytics.v_dlq_summary AS
SELECT * FROM iceberg.agg.agg_dlq_summary_hourly;

CREATE OR REPLACE VIEW iceberg.analytics.v_screening_outcomes AS
SELECT * FROM iceberg.facts.fact_screening_outcomes;
