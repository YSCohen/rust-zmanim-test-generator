package io.github.YSCohen.rustZmanimTestGenerator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.google.common.base.CaseFormat;
import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

/** Shared configuration, name mangling, and formatting used by every test generator. */
public class Helpers {
    /** The date every generated test is evaluated on (mirrors {@code civil::date(2017, 10, 17)} on the Rust side). */
    static final LocalDate SAMPLE_DATE = LocalDate.of(2017, 10, 17);

    /** Placeholder emitted for an absent (null) result, matching {@code None} on the Rust side. */
    static final String NONE = "None";

    private Helpers() {
    }

    /** Whether the generator was invoked with the {@code elev} argument, i.e. elevation-adjusted calculations. */
    static boolean useElevation(String[] args) {
        return args.length > 0 && args[0].equals("elev");
    }

    /** A calendar for {@code loc} configured for {@link #SAMPLE_DATE} and the given elevation mode. */
    static ComprehensiveZmanimCalendar newCzc(GeoLocation loc, boolean useElevation) {
        ComprehensiveZmanimCalendar czc = new ComprehensiveZmanimCalendar(loc);
        czc.setLocalDate(SAMPLE_DATE);
        czc.setUseElevation(useElevation);
        return czc;
    }

    /** The locations exercised by every generated test, in the same order as {@code test_helper::} */
    static GeoLocation[] getLocs() {
        GeoLocation lakewood = new GeoLocation(
                "LW",
                40.0721087,
                -74.2400243,
                15,
                ZoneId.of("America/New_York"));

        GeoLocation jerusalem = new GeoLocation(
                "JM",
                31.7781161,
                35.233804,
                740.0,
                ZoneId.of("Asia/Jerusalem"));

        GeoLocation losAngeles = new GeoLocation(
                "LA",
                34.0201613,
                -118.6919095,
                71,
                ZoneId.of("America/Los_Angeles"));

        GeoLocation tokyo = new GeoLocation(
                "TK",
                35.6733227,
                139.6403486,
                40,
                ZoneId.of("Asia/Tokyo"));

        GeoLocation arcticNunavut = new GeoLocation(
                "AN",
                81.7449398,
                -64.7945858,
                127,
                ZoneId.of("America/Toronto"));

        GeoLocation samoa = new GeoLocation(
                "SM",
                -13.8599098,
                -171.8031745,
                1858,
                ZoneId.of("Pacific/Apia"));

        GeoLocation fiji = new GeoLocation(
                "FJ",
                -17.633056,
                178.016667,
                1324,
                ZoneId.of("Pacific/Fiji"));

        GeoLocation honolulu = new GeoLocation(
                "HU",
                21.466667,
                -157.966667,
                10,
                ZoneId.of("America/Adak"));

        GeoLocation niue = new GeoLocation(
                "NI",
                -19.053006,
                -169.859199,
                75,
                ZoneId.of("Pacific/Niue"));

        return new GeoLocation[] {
                lakewood,
                jerusalem,
                losAngeles,
                tokyo,
                arcticNunavut,
                samoa,
                fiji,
                honolulu,
                niue
        };
    }

    /** Formats {@code instant} in {@code zoneId} with {@code pattern}, or {@link #NONE} if it is null. */
    static String formatDate(Instant instant, ZoneId zoneId, String pattern) {
        if (instant == null) {
            return NONE;
        }
        return instant.atZone(zoneId).format(DateTimeFormatter.ofPattern(pattern));
    }

    /** Formats {@code duration} truncated to milliseconds (ISO-8601), or {@link #NONE} if it is null. */
    static String formatDuration(Duration duration) {
        if (duration == null) {
            return NONE;
        }
        return duration.truncatedTo(ChronoUnit.MILLIS).toString();
    }

    /**
     * The shared first pass of method-name mangling: strip the {@code get} prefix and convert the KosherJava
     * {@code lowerCamelCase} getter name to {@code lower_snake_case}, separating a digit from a preceding letter.
     */
    static String baseRustName(String methodName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, methodName)
                .replace("get_", "")
                .replaceAll("([a-z])(\\d)", "$1_$2");
    }

    /** Renders {@code elements} as comma-terminated, 8-space-indented lines forming a Rust array-literal body. */
    static String arrayBody(String[] elements) {
        StringBuilder body = new StringBuilder();
        for (String element : elements) {
            body.append("        ").append(element).append(",\n");
        }
        return body.toString();
    }

    /** Like {@link #arrayBody}, but wraps each value in double quotes. */
    static String quotedArrayBody(String[] values) {
        String[] quoted = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            quoted[i] = "\"" + values[i] + "\"";
        }
        return arrayBody(quoted);
    }

    static void printHeader() {
        System.out.println("""
                // Generated by rust-zmanim-test-generator
                // https://github.com/YSCohen/rust-zmanim-test-generator
                // This file is part of rust-zmanim, licensed under LGPL-2.1
                """);
    }
}
