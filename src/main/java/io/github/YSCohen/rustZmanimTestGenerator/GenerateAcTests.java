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
                use jiff::civil;
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
            String[] results = new String[locs.length];
            for (int i = 0; i < locs.length; i++) {
                AstronomicalCalendar ac = new AstronomicalCalendar(locs[i]);
                ac.setLocalDate(Helpers.SAMPLE_DATE);

                Method method = ac.getClass().getMethod(javaMethodName);
                Instant value = (Instant) method.invoke(ac);
                results[i] = Helpers.formatDate(value, locs[i].getZoneId(), "yyyy-MM-dd HH:mm:ss.SSS xx");
            }

            System.out.printf(
                    """

                            #[test]
                            fn test_%s() {
                                let locations = test_helper::more_locations();
                                let expected_datetime_strs = [
                            %s    ];

                                for (loc, expected) in zip(locations, expected_datetime_strs) {
                                    let date = civil::date(2017, 10, 17);
                                    let actual = match astronomical_calculator::%s(date, &loc) {
                                        Some(dt) => dt.strftime("%s").to_string(),
                                        None => String::from("None"),
                                    };
                                    assert_eq!(expected, actual)
                                }
                            }
                            """,
                    rustFnName, Helpers.quotedArrayBody(results),
                    rustFnName, "%Y-%m-%d %H:%M:%S.%3f %z");
        } catch (Exception e) {
            System.out.println("\n// Could not invoke " + javaMethodName + " because " + e.getMessage());
        }
    }
}
