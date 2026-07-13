package io.github.YSCohen.rustZmanimTestGenerator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;

import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateAcTests {
    public static void main(String[] args) throws IOException {
        StringBuilder content = new StringBuilder("""
                //! this is a set of tests for
                //! [astronomical_calculator](rust_zmanim::astronomical_calculator)

                mod test_helper;
                use rust_zmanim::astronomical_calculator;
                use std::iter::zip;
                """);

        GeoLocation[] locs = Helpers.getLocs();

        content.append(generateSingleTest(locs, "getSunrise", "sunrise"));
        content.append(generateSingleTest(locs, "getSeaLevelSunrise", "sea_level_sunrise"));

        content.append(generateSingleTest(locs, "getSunset", "sunset"));
        content.append(generateSingleTest(locs, "getSeaLevelSunset", "sea_level_sunset"));

        content.append(generateSingleTest(locs, "getSunTransit", "solar_noon"));
        content.append(generateSingleTest(locs, "getSolarMidnight", "solar_midnight"));

        Helpers.writeTestFile(Path.of(args[0]), "test_ac_generated.rs", content.toString());
    }

    private static String generateSingleTest(GeoLocation[] locs, String javaMethodName, String rustFnName) {
        try {
            String[][] results = new String[locs.length][Helpers.SAMPLE_DATES.length];
            for (int i = 0; i < locs.length; i++) {
                AstronomicalCalendar ac = new AstronomicalCalendar(locs[i]);
                Method method = ac.getClass().getMethod(javaMethodName);

                for (int j = 0; j < Helpers.SAMPLE_DATES.length; j++) {
                    ac.setLocalDate(Helpers.SAMPLE_DATES[j]);
                    Instant value = (Instant) method.invoke(ac);
                    results[i][j] = Helpers.formatDate(value, locs[i].getZoneId(), "yyyy-MM-dd HH:mm:ss xx");
                }
            }

            return String.format(
                    """

                            #[test]
                            fn test_%s() {
                                let expected_datetime_strs = [
                            %s    ];

                                for ((loc, label), per_loc) in zip(
                                    zip(test_helper::more_locations(), test_helper::location_labels()),
                                    expected_datetime_strs,
                                ) {
                                    for (date, expected) in zip(test_helper::sample_dates(), per_loc) {
                                        let actual = match astronomical_calculator::%s(date, &loc) {
                                            Some(dt) => dt.strftime("%s").to_string(),
                                            None => String::from("None"),
                                        };
                                        assert_eq!(expected, actual, "at {label} on {date}");
                                    }
                                }
                            }
                            """,
                    rustFnName, Helpers.nestedQuotedArrayBody(results, locs),
                    rustFnName, "%Y-%m-%d %H:%M:%S %z");
        } catch (Exception e) {
            return "\n// Could not invoke " + javaMethodName + " because " + e.getMessage() + "\n";
        }
    }
}
