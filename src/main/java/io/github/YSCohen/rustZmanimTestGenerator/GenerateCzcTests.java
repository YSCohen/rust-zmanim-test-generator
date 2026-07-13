package io.github.YSCohen.rustZmanimTestGenerator;

import java.lang.reflect.Method;
import java.time.Instant;

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateCzcTests {
    public static void main(String[] args) {
        boolean useElevation = Helpers.useElevation(args);

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
        for (Method method : new ComprehensiveZmanimCalendar().getClass().getMethods()) {
            if (isZmanGetter(method)) { // if skipped, isZmanGetter will print why
                generateSingleZmanTest(locs, method, useElevation);
            }
        }
    }

    private static void generateSingleZmanTest(GeoLocation[] locs, Method method, boolean useElevation) {
        try {
            String[] results = new String[locs.length];
            for (int i = 0; i < locs.length; i++) {
                Instant value = (Instant) method.invoke(Helpers.newCzc(locs[i], useElevation));
                results[i] = Helpers.formatDate(value, locs[i].getZoneId(), "yyyy-MM-dd HH:mm:ss xx");
            }

            String modifiedName = transformMethodName(method.getName());

            System.out.printf(
                    """

                            #[test]
                            fn test_%s() {
                                let cals = test_helper::more_locations_czcs(%b);
                                let expected_datetime_strs = [
                            %s    ];

                                for (czc, expected) in zip(cals, expected_datetime_strs) {
                                    let actual = czc.%s().map_or_else(
                                        || String::from("None"),
                                        |dt| dt.strftime("%s").to_string(),
                                    );
                                    assert_eq!(expected, actual)
                                }
                            }
                            """,
                    modifiedName, useElevation, Helpers.quotedArrayBody(results),
                    modifiedName, "%Y-%m-%d %H:%M:%S %z");
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
                // for some reason it generates duplicate tests for chtzos
                // halayla, but if i put this filter it only generates one
                || method.getName().equals("getChatzosHalayla")
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
                || method.getName().contains("TeshuvosVehanhagos")
                || method.getName().contains("Twilight")
                || method.getName().contains("Transit")) {
            System.out.println(
                    "\n// Skipped " + method.getName() + " because it contains a phrase which was explicitly excluded");
            return false;
        }

        return true;
    }

    private static String transformMethodName(String methodName) {
        return Helpers.baseRustName(methodName)
                .replaceAll("(\\d)$", "$1_minutes")
                .replaceAll("(mincha.{0,12}?)_(\\d)", "$1_mga_$2")
                .replaceAll("(sof.{0,14}?)_(\\d)", "$1_mga_$2")
                .replaceAll("plag_(\\d)", "plag_mga_$2")
                .replaceAll("mga_(\\d)_hours", "$1_hrs")
                .replaceAll("(\\d)_zmanis", "$1_minutes_zmanis")
                .replace("_point", "")
                .replaceAll("([\\d_]+)_degrees_to_(.+)_geonim_([\\d_]+)_degrees", "$1_to_$2_$3")
                .replace("g_r_a", "gra")
                .replace("m_g_a", "mga")
                .replaceAll("mga_([\\d_]+)_degrees_to_fixed_local_chatzos", "mga_alos_$1_to_fixed_local_chatzos")
                .replace("r_t", "rt")
                .replace("shma", "shema")
                .replace("tfila", "tefila")
                .replace("plag_hamincha", "plag")
                .replace("bain", "bein")
                .replace("tzais", "tzeis")
                .replace("le_mincha", "lemincha")
                .replace("solar_midnight", "chatzos_halayla")
                .replace("plag_alos_to_sunset", "plag_alos_16_1_to_sunset")
                .replace("mincha_gedola_mga_30_minutes", "mincha_gedola_30_minutes")
                .replace("greaterthan", "greater_than")
                .replace("gedola_greater", "gedola_gra_greater")
                .replace("sunrise_with_elevation", "elevation_sunrise")
                .replace("sunset_with_elevation", "elevation_sunset")
                .replace("hashemashos", "hashmashos");
    }
}
