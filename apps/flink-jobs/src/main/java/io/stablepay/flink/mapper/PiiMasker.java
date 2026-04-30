package io.stablepay.flink.mapper;

public final class PiiMasker {

    private static final int VISIBLE_PREFIX_LENGTH = 4;
    private static final String MASK_CHAR = "*";

    private PiiMasker() {}

    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= VISIBLE_PREFIX_LENGTH) {
            return MASK_CHAR.repeat(value.length());
        }
        return value.substring(0, VISIBLE_PREFIX_LENGTH) + MASK_CHAR.repeat(value.length() - VISIBLE_PREFIX_LENGTH);
    }
}
