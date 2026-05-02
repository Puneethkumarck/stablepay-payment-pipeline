package io.stablepay.api.infrastructure.cursor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class Base64PipeCursor {

  private Base64PipeCursor() {}

  public static String encode(long longPart, String stringPart) {
    Objects.requireNonNull(stringPart, "stringPart");
    var raw = longPart + "|" + stringPart;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static Base64PipeCursorPart decode(String cursor, String errorCode) {
    Objects.requireNonNull(cursor, "cursor");
    Objects.requireNonNull(errorCode, "errorCode");
    try {
      var decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      var pipe = decoded.indexOf('|');
      if (pipe <= 0 || pipe == decoded.length() - 1) {
        throw new IllegalArgumentException(errorCode + " malformed cursor payload");
      }
      var longPart = Long.parseLong(decoded.substring(0, pipe));
      var stringPart = decoded.substring(pipe + 1);
      return new Base64PipeCursorPart(longPart, stringPart);
    } catch (IllegalArgumentException e) {
      if (e.getMessage() != null && e.getMessage().startsWith(errorCode)) {
        throw e;
      }
      throw new IllegalArgumentException(errorCode + " invalid cursor", e);
    }
  }
}
