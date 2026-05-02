package io.stablepay.api.infrastructure.opensearch;

import io.stablepay.api.infrastructure.cursor.Base64PipeCursor;

final class OpenSearchCursorCodec {

  static final String DECODE_ERROR_CODE = "STBLPAY-2002";

  private OpenSearchCursorCodec() {}

  static String encode(long eventTimeMillis, String eventId) {
    return Base64PipeCursor.encode(eventTimeMillis, eventId);
  }

  static OpenSearchCursorValue decode(String cursor) {
    var part = Base64PipeCursor.decode(cursor, DECODE_ERROR_CODE);
    return new OpenSearchCursorValue(part.longPart(), part.stringPart());
  }
}
