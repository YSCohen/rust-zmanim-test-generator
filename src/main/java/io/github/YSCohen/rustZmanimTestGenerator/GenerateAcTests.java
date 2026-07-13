package io.github.YSCohen.rustZmanimTestGenerator;

import java.lang.reflect.Method;
import java.time.Instant;

import com.kosherjava.zmanim.AstronomicalCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateAcTests {
    public static void main(String[] args) {
        Helpers.printHeader();
        System.out.print("""
                //! this is a set of tests for
                //! [astronomical_calculator](rust_zmanim::astronomical_calculator)

                mod test_helper;
                use rust_zmanim::astronomical_calculator;
                use std::iter::zip;
                """);

        GeoLocation[] locs = Helpers.getLocs();

        generateSingleTest(locs, "getSunrise", "sunrise");
        generateSingleTest(locs, "getSeaLevelSunrise", "sea_level_sunrise");

        generateSingleTest(locs, "getSunset", "sunset");
        generateSingleTest(locs, "getSeaLevelSunset", "sea_level_sunset");

        generateSingleTest(locs, "getSunTransit", "solar_noon");
        generateSingleTest(locs, "getSolarMidnight", "solar_midnight");
    }

    private static void generateSingleTest(GeoLocation[] locs, String javaMethodName, String rustFnName) {
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

            System.out.printf(
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
            System.out.println("\n// Could not invoke " + javaMethodName + " because " + e.getMessage());
        }
    }
}
