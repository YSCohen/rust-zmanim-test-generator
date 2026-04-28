package io.github.YSCohen.rustZmanimTestGenerator;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;

import com.google.common.base.CaseFormat;
import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateCzcTests {
    public static void main(String[] args) {
        boolean elev = (args.length > 0 && args[0].equals("elev"));
        generateAllZmanTests(elev);
    }

    private static void generateAllZmanTests(boolean useElevation) {
        Helpers.printHeader();
        System.out.printf("""
                //! this is a set of tests for
                //! [ComplexZmanimCalendar](rust_zmanim::complex_zmanim_calendar::ComplexZmanimCalendar),
                //! using %s calculations

                mod test_helper;
                use std::iter::zip;
                """,
                useElevation ? "elevation-adjusted" : "sea-level");

        GeoLocation[] locs = Helpers.getLocs();
        Method[] methods = new ComprehensiveZmanimCalendar().getClass().getMethods();

        for (Method method : methods) {
            if (isZmanGetter(method)) { // if skipped, isZmanGetter will print why
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

                Instant value = (Instant) method.invoke(czcOfLocation);
                results[i] = Helpers.formatDate(value, locs[i].getZoneId(), "yyyy-MM-dd HH:mm:ss z");
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
                                        |dt| dt.strftime("%s").to_string(),
                                    );
                                    assert_eq!(result, edt)
                                }
                            }
                            """,
                    modifiedName, useElevation, results[0], results[1], results[2],
                    results[3], results[4], results[5], results[6], results[7], results[8],
                    modifiedName, "%Y-%m-%d %H:%M:%S %Z");
        } catch (Exception e) {
            System.out.println("\n// Could not invoke " + method.getName() + " because " + e.getMessage());
        }
    }

    private static boolean isZmanGetter(Method method) {
        if (!method.getName().startsWith("get")) {
            System.out.println("\n// Skipped " + method.getName() + " because it isn't a getter");
            return false;
        }

        if (method.getParameterCount() != 0) {
            System.out.println("\n// Skipped " + method.getName() + " because it takes parameters");
            return false;
        }

        if (!Instant.class.equals(method.getReturnType())) {
            System.out.println("\n// Skipped " + method.getName() + " because it doesn't return an Instant");
            return false;

        }

        if (method.getName().equals("getClass")
                || method.getName().equals("getSunset")
                || method.getName().equals("getSunrise")
                || method.getName().equals("getSeaLevelSunset")
                || method.getName().equals("getSeaLevelSunrise")
                || method.getName().equals("getAlosHashachar")
                || method.getName().equals("getTzais")
                || method.getName().equals("getMinchaGedola")
                || method.getName().equals("getMinchaKetana")
                || method.getName().equals("getPlagHamincha")
                || method.getName().equals("getSofZmanShmaMGA")
                || method.getName().equals("getCandleLighting")
                || method.getName().equals("getSofZmanTfilaMGA")
                || method.getName().equals("getChatzosAsHalfDay")) {
            System.out.println(
                    "\n// Skipped " + method.getName() + " because it is one of the explicitly excluded methods");
            return false;
        }

        if (method.getName().contains("Mol") || method.getName().contains("Levana")) {
            System.out.println(
                    "\n// Skipped " + method.getName() + " because this library doesn't calculate molados (yet?)");
            return false;
        }

        if (method.getName().contains("Chametz")
                || method.getName().contains("Twilight")
                || method.getName().contains("Transit")) {
            System.out.println(
                    "\n// Skipped " + method.getName() + " because it contains a phrase which was explicitly excluded");
            return false;
        }

        return true;
    }

    private static String transformMethodName(String methodName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, methodName)
                .replace("get_", "")
                .replaceAll("([a-z])(\\d)", "$1_$2")
                .replaceAll("(\\d)$", "$1_minutes")
                .replaceAll("(mincha.{0,12}?)_(\\d)", "$1_mga_$2")
                .replaceAll("(sof.{0,14}?)_(\\d)", "$1_mga_$2")
                .replaceAll("plag_(\\d)", "plag_mga_$2")
                .replaceAll("mga_(\\d)_hours", "$1_hrs")
                .replaceAll("(\\d)_zmanis", "$1_minutes_zmanis")
                .replace("g_r_a", "gra")
                .replace("m_g_a", "mga")
                .replace("r_t", "rt")
                .replace("_point", "")
                .replace("shma", "shema")
                .replace("tfila", "tefila")
                .replace("plag_hamincha", "plag")
                .replace("bain", "bein")
                .replace("tzais", "tzeis")
                .replace("le_mincha", "lemincha")
                .replace("solar_midnight", "chatzos_halayla")
                .replace("plag_alos_to_sunset", "plag_alos_16_1_degrees_to_sunset")
                .replace("mincha_gedola_mga_30_minutes", "mincha_gedola_30_minutes")
                .replace("alos_16_1_to", "alos_16_1_degrees_to")
                .replace("greaterthan", "greater_than")
                .replace("gedola_greater", "gedola_gra_greater")
                .replace("sunrise_with_elevation", "elevation_sunrise")
                .replace("sunset_with_elevation", "elevation_sunset")
                .replace("hashemashos", "hashmashos");
    }
}
