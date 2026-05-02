package io.stablepay.api.infrastructure.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.neovisionaries.i18n.CurrencyCode;
import io.stablepay.api.domain.model.Money;
import io.stablepay.api.domain.model.TransactionId;
import io.stablepay.api.domain.model.fixtures.TransactionFixtures;
import io.stablepay.api.infrastructure.opensearch.fixtures.OpenSearchTransactionDocumentFixtures;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OpenSearchDocumentMapperTest {

  private final OpenSearchDocumentMapper mapper = Mappers.getMapper(OpenSearchDocumentMapper.class);

  @Test
  void shouldMapDocumentBackToCanonicalDomainTransaction() {
    // given
    var document = OpenSearchTransactionDocumentFixtures.SOME_OS_TRANSACTION_DOCUMENT;
    var expected =
        TransactionFixtures.someTransaction()
            .id(TransactionId.of(UUID.fromString(document.eventId())))
            .eventId(document.eventId())
            .counterparty(Optional.<String>empty())
            .typedFields(Map.<String, Object>of())
            .build();

    // when
    var actual = mapper.toDomain(document);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void shouldMapAlternateCurrencyAndStatusValues() {
    // given
    var document =
        OpenSearchTransactionDocumentFixtures.someOpenSearchDocument()
            .currencyCode("EUR")
            .internalStatus("PENDING")
            .customerStatus("PROCESSING")
            .amountMicros(250_500_000L)
            .build();
    var expectedAmount = Money.fromMicros(250_500_000L, CurrencyCode.EUR);
    var expected =
        TransactionFixtures.someTransaction()
            .id(TransactionId.of(UUID.fromString(document.eventId())))
            .eventId(document.eventId())
            .amount(expectedAmount)
            .internalStatus("PENDING")
            .customerStatus("PROCESSING")
            .counterparty(Optional.<String>empty())
            .typedFields(Map.<String, Object>of())
            .build();

    // when
    var actual = mapper.toDomain(document);

    // then
    assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  }
}
