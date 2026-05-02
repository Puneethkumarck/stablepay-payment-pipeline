package io.stablepay.api.infrastructure.opensearch;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Encodes and decodes opaque pagination cursors used by the OpenSearch adapter. Cursors are
 * URL-safe base64 over the literal {@code <event_time_millis>|<event_id>} payload so the search
 * adapter can call {@code search_after} on each page.
 */
final class OpenSearchCursorCodec {

  static final String DECODE_ERROR_CODE = "STBLPAY-2002";

  private OpenSearchCursorCodec() {}

  static String encode(long eventTimeMillis, String eventId) {
    Objects.requireNonNull(eventId, "eventId");
    var raw = eventTimeMillis + "|" + eventId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  static OpenSearchCursorValue decode(String cursor) {
    Objects.requireNonNull(cursor, "cursor");
    try {
      var decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      var pipe = decoded.indexOf('|');
      if (pipe <= 0 || pipe == decoded.length() - 1) {
        throw new IllegalArgumentException(DECODE_ERROR_CODE + " malformed cursor payload");
      }
      var eventTimeMillis = Long.parseLong(decoded.substring(0, pipe));
      var eventId = decoded.substring(pipe + 1);
      return new OpenSearchCursorValue(eventTimeMillis, eventId);
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().startsWith(DECODE_ERROR_CODE)) {
        throw e;
      }
      throw new IllegalArgumentException(DECODE_ERROR_CODE + " invalid cursor", e);
    }
  }

  record OpenSearchCursorValue(long eventTimeMillis, String eventId) {
    OpenSearchCursorValue {
      Objects.requireNonNull(eventId, "eventId");
    }
  }
}
