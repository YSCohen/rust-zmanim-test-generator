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
    /** The instant (UTC) at which the solar position is sampled. */
    private static final String INSTANT = "2017-10-17T12:34:56.789Z";

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

                fn instant() -> jiff::Zoned {
                    \"""" + INSTANT + """
                \"
                        .parse::<Timestamp>()
                        .unwrap()
                        .to_zoned(TimeZone::UTC)
                }
                """);

        GeoLocation[] locs = Helpers.getLocs();
        NOAACalculator calc = new NOAACalculator();
        Instant instant = Instant.parse(INSTANT);

        double[] azimuths = new double[locs.length];
        double[] elevations = new double[locs.length];
        for (int i = 0; i < locs.length; i++) {
            azimuths[i] = calc.getSolarAzimuth(instant, locs[i]);
            elevations[i] = calc.getSolarElevation(instant, locs[i]);
        }

        printTest("test_ac_solar_azimuth", "astronomical_calculator::solar_azimuth(&instant(), &loc)",
                "test_helper::more_locations()", "loc", azimuths);
        printTest("test_ac_solar_elevation", "astronomical_calculator::solar_elevation(&instant(), &loc)",
                "test_helper::more_locations()", "loc", elevations);
        printTest("test_czc_solar_azimuth", "czc.solar_azimuth(&instant())",
                "test_helper::more_locations_czcs(false)", "czc", azimuths);
        printTest("test_czc_solar_elevation", "czc.solar_elevation(&instant())",
                "test_helper::more_locations_czcs(false)", "czc", elevations);
    }

    private static void printTest(String testName, String call, String source, String binding,
            double[] values) {
        String[] elements = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            elements[i] = String.valueOf(values[i]);
        }

        System.out.printf(
                """

                        #[test]
                        fn %s() {
                            let items = %s;
                            let expected = [
                        %s    ];

                            for (%s, exp) in zip(items, expected) {
                                let actual = %s;
                                assert!(
                                    (actual - exp).abs() < TOLERANCE,
                                    "expected {exp}, got {actual}"
                                );
                            }
                        }
                        """,
                testName, source, Helpers.arrayBody(elements), binding, call);
    }
}
