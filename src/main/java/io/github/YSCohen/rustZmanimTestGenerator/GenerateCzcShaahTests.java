package io.github.YSCohen.rustZmanimTestGenerator;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.google.common.base.CaseFormat;
import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateCzcShaahTests {
    public static void main(String[] args) {
        boolean elev = (args.length > 0 && args[0].equals("elev"));
        generateAllZmanTests(elev);
    }

    private static void generateAllZmanTests(boolean useElevation) {
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
        Method[] methods = new ComprehensiveZmanimCalendar().getClass().getMethods();

        for (Method method : methods) {
            if (isShaahGetter(method)) {
                generateSingleZmanTest(locs, method, useElevation);
            }
        }
    }

    private static void generateSingleZmanTest(GeoLocation[] locs, Method method, boolean useElevation) {
        try {
            String[] results = new String[locs.length];
            LocalDate ld = LocalDate.of(2017, 10, 17);
            for (int i = 0; i < locs.length; i++) {
                ComprehensiveZmanimCalendar czcOfLocation = new ComprehensiveZmanimCalendar(locs[i]);
                czcOfLocation.setLocalDate(ld);
                czcOfLocation.setUseElevation(useElevation);

                Duration value = (Duration) method.invoke(czcOfLocation);
                results[i] = formatDuration(value);
            }

            String modifiedName = transformMethodName(method.getName());

            System.out.printf(
                    """

                            #[test]
                            fn test_%s() {
                                let cals = test_helper::more_locations_czcs(%b);
                                let expected_datetime_strs = [
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                ];

                                for (czc, edt) in zip(cals, expected_datetime_strs) {
                                    let result = czc.%s().map_or_else(
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
                                    assert_eq!(result, edt)
                                }
                            }
                            """,
                    modifiedName, useElevation,
                    results[0], results[1], results[2], results[3], results[4],
                    results[5], results[6], results[7], results[8],
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
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, methodName)
                .replace("get_", "")
                .replaceAll("([a-z])(\\d)", "$1_$2")
                .replaceAll("zmanis_(\\d)", "zmanis_mga_$1")
                // .replaceAll("(\\d)$", "$1_minutes")
                .replace("_point", "")
                .replace("tzais", "tzeis")
                .replace("g_r_a", "gra");
    }

    private static String formatDuration(Duration d) {
        if (d == null) {
            return "None";
        } else {
            return d.truncatedTo(ChronoUnit.MILLIS).toString();
        }
    }
}
