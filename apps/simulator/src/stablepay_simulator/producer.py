from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import structlog
from confluent_kafka import KafkaException, Producer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import MessageField, SerializationContext

logger = structlog.get_logger()

SCHEMA_DIR = Path(__file__).resolve().parents[5] / "schemas" / "src" / "main" / "avro"

TOPIC_MAP: dict[str, str] = {
    "fiat_payout": "payment.payout.fiat.v1",
    "crypto_payout": "payment.payout.crypto.v1",
    "fiat_payin": "payment.payin.fiat.v1",
    "crypto_payin": "payment.payin.crypto.v1",
    "multi_leg_flow": "payment.flow.v1",
    "chain_transaction": "chain.transaction.v1",
    "signing_request": "signing.request.v1",
    "screening_result": "screening.result.v1",
    "approval_decision": "approval.decision.v1",
}

SCHEMA_FILE_MAP: dict[str, str] = {
    "PayoutFiatV1": "payout-fiat/payment_payout_fiat.avsc",
    "PayoutCryptoV1": "payout-crypto/payment_payout_crypto.avsc",
    "PayinFiatV1": "payin-fiat/payment_payin_fiat.avsc",
    "PayinCryptoV1": "payin-crypto/payment_payin_crypto.avsc",
    "PaymentFlowV1": "flow/payment_flow.avsc",
    "ChainTransactionV1": "chain/chain_transaction.avsc",
    "SigningRequestV1": "signing/signing_request.avsc",
    "ScreeningResultV1": "screening/screening_result.avsc",
    "ApprovalDecisionV1": "approval/approval_decision.avsc",
}

COMMON_SCHEMAS = {
    "common/event_envelope.avsc",
    "common/money.avsc",
    "common/party.avsc",
    "common/address.avsc",
}


def _load_avro_schema_str(schema_name: str) -> str:
    rel = SCHEMA_FILE_MAP[schema_name]
    schema_path = SCHEMA_DIR / rel
    schema = json.loads(schema_path.read_text())

    common_dir = SCHEMA_DIR / "common"
    referenced_schemas = []
    for common_file in sorted(COMMON_SCHEMAS):
        common_path = SCHEMA_DIR / common_file
        if common_path.exists():
            referenced_schemas.append(json.loads(common_path.read_text()))

    if schema_name == "PaymentFlowV1":
        pass

    return json.dumps(schema)


class TransactionalAvroProducer:
    def __init__(
        self,
        bootstrap_servers: str,
        schema_registry_url: str,
        transactional_id: str = "stablepay-simulator-tx",
    ) -> None:
        self._bootstrap_servers = bootstrap_servers
        self._schema_registry_url = schema_registry_url
        self._sr_client = SchemaRegistryClient({"url": schema_registry_url})
        self._serializers: dict[str, AvroSerializer] = {}

        self._producer = Producer(
            {
                "bootstrap.servers": bootstrap_servers,
                "enable.idempotence": True,
                "transactional.id": transactional_id,
                "acks": "all",
                "linger.ms": 5,
                "batch.size": 65536,
                "queue.buffering.max.messages": 100000,
                "compression.type": "zstd",
            }
        )

    def init(self) -> None:
        self._producer.init_transactions()
        logger.info("kafka_producer_initialized", transactional=True)

    def _get_serializer(self, schema_name: str) -> AvroSerializer:
        if schema_name not in self._serializers:
            schema_str = _load_avro_schema_str(schema_name)
            self._serializers[schema_name] = AvroSerializer(
                self._sr_client,
                schema_str,
                conf={"auto.register.schemas": True},
            )
        return self._serializers[schema_name]

    def produce_batch(self, events: list[tuple[str, str, str, dict[str, Any]]]) -> None:
        self._producer.begin_transaction()
        try:
            for topic, key, schema_name, record in events:
                serializer = self._get_serializer(schema_name)
                ctx = SerializationContext(topic, MessageField.VALUE)
                value_bytes = serializer(record, ctx)
                self._producer.produce(
                    topic=topic,
                    key=key.encode("utf-8"),
                    value=value_bytes,
                    on_delivery=self._delivery_callback,
                )
            self._producer.flush(timeout=30.0)
            self._producer.commit_transaction()
        except KafkaException:
            logger.exception("kafka_transaction_failed")
            self._producer.abort_transaction()
            raise

    @staticmethod
    def _delivery_callback(err: Any, msg: Any) -> None:
        if err is not None:
            logger.error("kafka_delivery_failed", error=str(err), topic=msg.topic())
        else:
            logger.debug(
                "kafka_delivery_ok",
                topic=msg.topic(),
                partition=msg.partition(),
                offset=msg.offset(),
            )

    def close(self) -> None:
        self._producer.flush(timeout=30.0)
        logger.info("kafka_producer_closed")
