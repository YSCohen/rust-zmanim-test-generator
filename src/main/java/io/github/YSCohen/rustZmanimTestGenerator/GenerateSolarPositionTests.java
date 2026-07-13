package io.github.YSCohen.rustZmanimTestGenerator;

import java.time.Instant;

import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.util.NOAACalculator;

/**
 * Generates tests for the public solar azimuth / elevation functions of both
 * {@code astronomical_calculator} and {@code ComplexZmanimCalendar}, using
 * KosherJava's {@link NOAACalculator} as the reference implementation.
 */
public class GenerateSolarPositionTests {
    /** The UTC time of day at which the solar position is sampled on each of {@link Helpers#SAMPLE_DATES}. */
    private static final String TIME_SUFFIX = "T12:34:56.789Z";

    public static void main(String[] args) {
        Helpers.printHeader();
        System.out.print("""
                //! this is a set of tests for the solar azimuth and elevation functions of
                //! [astronomical_calculator](rust_zmanim::astronomical_calculator) and
                //! [ComplexZmanimCalendar](rust_zmanim::complex_zmanim_calendar::ComplexZmanimCalendar)

                mod test_helper;
                use jiff::{Timestamp, tz::TimeZone};
                use rust_zmanim::astronomical_calculator;
                use std::iter::zip;

                /// Maximum acceptable variation, in degrees
                const TOLERANCE: f64 = 1e-12;

                /// The instants (12:34:56.789 UTC on each sample date) at which the solar
                /// position is sampled
                fn instants() -> [jiff::Zoned; 7] {
                    test_helper::sample_dates().map(|date| {
                        format!("{date}""" + TIME_SUFFIX + """
                ")
                            .parse::<Timestamp>()
                            .unwrap()
                            .to_zoned(TimeZone::UTC)
                    })
                }
                """);

        GeoLocation[] locs = Helpers.getLocs();
        NOAACalculator calc = new NOAACalculator();

        double[][] azimuths = new double[locs.length][Helpers.SAMPLE_DATES.length];
        double[][] elevations = new double[locs.length][Helpers.SAMPLE_DATES.length];
        for (int i = 0; i < locs.length; i++) {
            for (int j = 0; j < Helpers.SAMPLE_DATES.length; j++) {
                Instant instant = Instant.parse(Helpers.SAMPLE_DATES[j] + TIME_SUFFIX);
                azimuths[i][j] = calc.getSolarAzimuth(instant, locs[i]);
                elevations[i][j] = calc.getSolarElevation(instant, locs[i]);
            }
        }

        printTest("test_ac_solar_azimuth", "astronomical_calculator::solar_azimuth(&instant, &loc)",
                false, azimuths, locs);
        printTest("test_ac_solar_elevation", "astronomical_calculator::solar_elevation(&instant, &loc)",
                false, elevations, locs);
        printTest("test_czc_solar_azimuth", "czc.solar_azimuth(&instant)",
                true, azimuths, locs);
        printTest("test_czc_solar_elevation", "czc.solar_elevation(&instant)",
                true, elevations, locs);
    }

    private static void printTest(String testName, String call, boolean useCzc, double[][] values,
            GeoLocation[] locs) {
        String[][] elements = new String[values.length][];
        for (int i = 0; i < values.length; i++) {
            elements[i] = new String[values[i].length];
            for (int j = 0; j < values[i].length; j++) {
                elements[i][j] = String.valueOf(values[i][j]);
            }
        }

        String setup = useCzc ? "    let mut czc = test_helper::single_czc(false);\n" : "";
        String repoint = useCzc ? "        czc.set_geo_location(loc);\n" : "";

        System.out.printf(
                """

                        #[test]
                        fn %s() {
                        %s    let expected = [
                        %s    ];

                            for ((loc, label), per_loc) in zip(
                                zip(test_helper::more_locations(), test_helper::location_labels()),
                                expected,
                            ) {
                        %s        for (instant, exp) in zip(instants(), per_loc) {
                                    let actual = %s;
                                    assert!(
                                        (actual - exp).abs() < TOLERANCE,
                                        "expected {exp}, got {actual} at {label} at {instant}"
                                    );
                                }
                            }
                        }
                        """,
                testName, setup, Helpers.nestedArrayBody(elements, locs), repoint, call);
    }
}
