package io.github.YSCohen.rustZmanimTestGenerator;

import java.lang.reflect.Method;
import java.time.Duration;

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateCzcShaahTests {
    public static void main(String[] args) {
        boolean useElevation = Helpers.useElevation(args);

        Helpers.printHeader();
        System.out.printf("""
                //! this is a set of tests for the *shaah zmanis* methods of
                //! [ComplexZmanimCalendar](rust_zmanim::complex_zmanim_calendar::ComplexZmanimCalendar),
                //! using %s calculations.

                mod test_helper;
                use jiff::{RoundMode, SignedDurationRound, Unit};
                use std::iter::zip;
                """,
                useElevation ? "elevation-adjusted" : "sea-level");

        GeoLocation[] locs = Helpers.getLocs();
        for (Method method : new ComprehensiveZmanimCalendar().getClass().getMethods()) {
            if (isShaahGetter(method)) {
                generateSingleZmanTest(locs, method, useElevation);
            }
        }
    }

    private static void generateSingleZmanTest(GeoLocation[] locs, Method method, boolean useElevation) {
        try {
            String[][] results = new String[locs.length][Helpers.SAMPLE_DATES.length];
            for (int i = 0; i < locs.length; i++) {
                for (int j = 0; j < Helpers.SAMPLE_DATES.length; j++) {
                    Duration value = (Duration) method
                            .invoke(Helpers.newCzc(locs[i], Helpers.SAMPLE_DATES[j], useElevation));
                    results[i][j] = Helpers.formatDuration(value);
                }
            }

            String modifiedName = transformMethodName(method.getName());

            System.out.printf(
                    """

                            #[test]
                            fn test_%s() {
                                let mut czc = test_helper::single_czc(%b);
                                let expected_duration_strs = [
                            %s    ];

                                for ((loc, label), per_loc) in zip(
                                    zip(test_helper::more_locations(), test_helper::location_labels()),
                                    expected_duration_strs,
                                ) {
                                    czc.set_geo_location(loc);
                                    for (date, expected) in zip(test_helper::sample_dates(), per_loc) {
                                        czc.set_date(date);
                                        let actual = czc.%s().map_or_else(
                                            || String::from("None"),
                                            |sd| {
                                                sd.round(
                                                    SignedDurationRound::new()
                                                        .smallest(Unit::Millisecond)
                                                        .mode(RoundMode::Trunc),
                                                )
                                                .unwrap()
                                                .to_string()
                                            },
                                        );
                                        assert_eq!(expected, actual, "at {label} on {date}");
                                    }
                                }
                            }
                            """,
                    modifiedName, useElevation, Helpers.nestedQuotedArrayBody(results, locs),
                    modifiedName);
        } catch (Exception e) {
            System.out.println("\n// Could not invoke " + method.getName() + " because " + e.getMessage());
        }
    }

    private static boolean isShaahGetter(Method method) {
        return method.getName().startsWith("getShaah")
                && method.getParameterCount() == 0
                && method.getReturnType() == Duration.class
                && !method.getName().equals("getShaahZmanisMGA");
    }

    private static String transformMethodName(String methodName) {
        return Helpers.baseRustName(methodName)
                .replaceAll("zmanis_(\\d)", "zmanis_mga_$1")
                .replace("_point", "")
                .replaceAll("([\\d_]+)_degrees_to_(.+)_geonim_([\\d_]+)_degrees", "$1_to_$2_$3")
                .replace("tzais", "tzeis")
                .replace("g_r_a", "gra");
    }
}
