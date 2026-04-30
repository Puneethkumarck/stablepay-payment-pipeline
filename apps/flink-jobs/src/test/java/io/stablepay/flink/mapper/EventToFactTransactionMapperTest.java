package io.stablepay.flink.mapper;

import static io.stablepay.flink.fixtures.FactFixtures.SOME_ACCOUNT_ID;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_CORRELATION_ID;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_CUSTOMER_ID;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_EVENT_ID;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_EVENT_TIME_MILLIS;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_FLOW_ID;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_INGEST_TIME_MILLIS;
import static io.stablepay.flink.fixtures.FactFixtures.SOME_TRACE_ID;
import static io.stablepay.flink.fixtures.FactFixtures.fiatPayoutEvent;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EventToFactTransactionMapperTest {

    @Test
    void shouldProduceRowDataWith41Fields() {
        // given
        var event = fiatPayoutEvent(5_000_000L, "USD", "COMPLETED");

        // when
        var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

        // then
        assertThat(result.getArity()).isEqualTo(41);
    }

    @Test
    void shouldMapAllFieldsForFiatPayout() {
        // given
        var event = fiatPayoutEvent(5_000_000L, "USD", "COMPLETED");

        // when
        var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

        // then
        var expected = new GenericRowData(41);
        expected.setField(0, StringData.fromString(SOME_EVENT_ID));
        expected.setField(1, TimestampData.fromInstant(Instant.ofEpochMilli(SOME_EVENT_TIME_MILLIS)));
        expected.setField(2, TimestampData.fromInstant(Instant.ofEpochMilli(SOME_INGEST_TIME_MILLIS)));
        expected.setField(3, StringData.fromString(SOME_FLOW_ID));
        expected.setField(4, StringData.fromString(SOME_CORRELATION_ID));
        expected.setField(5, StringData.fromString(SOME_TRACE_ID));
        expected.setField(6, StringData.fromString("PAYOUT_FIAT"));
        expected.setField(7, StringData.fromString("FIAT"));
        expected.setField(8, StringData.fromString("OUTBOUND"));
        expected.setField(9, false);
        expected.setField(10, null);
        expected.setField(11, StringData.fromString("PAY-REF-001"));
        expected.setField(12, StringData.fromString(SOME_CUSTOMER_ID));
        expected.setField(13, StringData.fromString(SOME_ACCOUNT_ID));
        expected.setField(14, 5_000_000L);
        expected.setField(15, StringData.fromString("USD"));
        expected.setField(16, null);
        expected.setField(17, null);
        expected.setField(18, null);
        expected.setField(19, null);
        expected.setField(20, null);
        expected.setField(21, null);
        expected.setField(22, null);
        expected.setField(23, StringData.fromString("COMPLETED"));
        expected.setField(24, StringData.fromString("PROCESSING"));
        expected.setField(25, null);
        expected.setField(26, null);
        expected.setField(27, null);
        expected.setField(28, null);
        expected.setField(29, null);
        expected.setField(30, null);
        expected.setField(31, null);
        expected.setField(32, null);
        expected.setField(33, null);
        expected.setField(34, null);
        expected.setField(35, StringData.fromString("provider-x"));
        expected.setField(36, StringData.fromString("route-1"));
        expected.setField(37, StringData.fromString(PiiMasker.mask("Jane Beneficiary")));
        expected.setField(38, StringData.fromString(PiiMasker.mask("John Sender")));
        expected.setField(39, StringData.fromString(PiiMasker.mask("Payment to vendor")));
        expected.setField(40, StringData.fromString(PiiMasker.mask("Monthly invoice")));

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Nested
    class CustomerIdIsNotMasked {

        @Test
        void shouldStoreCustomerIdUnmasked() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "EUR", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(12).toString()).isEqualTo(SOME_CUSTOMER_ID);
        }

        @Test
        void shouldStoreAccountIdUnmasked() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "EUR", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(13).toString()).isEqualTo(SOME_ACCOUNT_ID);
        }
    }

    @Nested
    class PiiFieldsAreMasked {

        @Test
        void shouldMaskBeneficiaryName() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "EUR", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(37).toString()).isEqualTo("Jane************");
        }

        @Test
        void shouldMaskDescription() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "EUR", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(39).toString()).startsWith("Paym");
            assertThat(result.getString(39).toString()).contains("*");
        }
    }

    @Nested
    class MoneyFieldsUseLong {

        @Test
        void shouldStoreAmountMicrosAsLong() {
            // given
            var event = fiatPayoutEvent(12_345_678L, "GBP", "COMPLETED");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getLong(14)).isEqualTo(12_345_678L);
        }

        @Test
        void shouldStoreCurrencyCodeAsString() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "GBP", "COMPLETED");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(15).toString()).isEqualTo("GBP");
        }
    }

    @Nested
    class DerivedFields {

        @Test
        void shouldDeriveEventTypeFromTopic() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "USD", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(6).toString()).isEqualTo("PAYOUT_FIAT");
        }

        @Test
        void shouldDeriveFlowTypeFiatFromFiatTopic() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "USD", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(7).toString()).isEqualTo("FIAT");
        }

        @Test
        void shouldDeriveDirectionOutboundFromPayoutTopic() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "USD", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getString(8).toString()).isEqualTo("OUTBOUND");
        }

        @Test
        void shouldSetIsCryptoFalseForFiatTopic() {
            // given
            var event = fiatPayoutEvent(1_000_000L, "USD", "PENDING");

            // when
            var result = (GenericRowData) EventToFactTransactionMapper.toRowData(event);

            // then
            assertThat(result.getBoolean(9)).isFalse();
        }
    }
}
