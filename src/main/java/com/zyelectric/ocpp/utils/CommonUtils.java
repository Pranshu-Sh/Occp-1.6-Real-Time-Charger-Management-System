package com.zyelectric.ocpp.utils;

import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Log4j2
public final class CommonUtils {

    private CommonUtils() {
    }

    public static String convertEpochToIso8601(final long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    public static long convertIso8601ToEpoch(final String iso8601) {
            Instant instant = Instant.parse(iso8601);
            return instant.toEpochMilli();
        }
}
