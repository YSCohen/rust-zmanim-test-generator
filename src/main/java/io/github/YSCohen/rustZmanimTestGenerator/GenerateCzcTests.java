package io.github.YSCohen.rustZmanimTestGenerator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;

import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

public class GenerateCzcTests {
    public static void main(String[] args) throws IOException {
        Path outDir = Path.of(args[0]);
        generate(outDir, false);
        generate(outDir, true);
    }

    private static void generate(Path outDir, boolean useElevation) throws IOException {
        StringBuilder content = new StringBuilder(String.format("""
                //! this is a set of tests for
                //! [ComplexZmanimCalendar](rust_zmanim::complex_zmanim_calendar::ComplexZmanimCalendar),
                //! using %s calculations

                mod test_helper;
                use std::iter::zip;
                """,
                useElevation ? "elevation-adjusted" : "sea-level"));

        GeoLocation[] locs = Helpers.getLocs();
        for (Method method : new ComprehensiveZmanimCalendar().getClass().getMethods()) {
            if (isZmanGetter(method, content)) { // if skipped, isZmanGetter will note why
                content.append(generateSingleZmanTest(locs, method, useElevation));
            }
        }

        Helpers.writeTestFile(outDir,
                useElevation ? "test_czc_generated_elevation.rs" : "test_czc_generated_sea_level.rs",
                content.toString());
    }

    private static String generateSingleZmanTest(GeoLocation[] locs, Method method, boolean useElevation) {
        try {
            String[][] results = new String[locs.length][Helpers.SAMPLE_DATES.length];
            for (int i = 0; i < locs.length; i++) {
                for (int j = 0; j < Helpers.SAMPLE_DATES.length; j++) {
                    Instant value = (Instant) method
                            .invoke(Helpers.newCzc(locs[i], Helpers.SAMPLE_DATES[j], useElevation));
                    results[i][j] = Helpers.formatDate(value, locs[i].getZoneId(), "yyyy-MM-dd HH:mm:ss xx");
                }
            }

            String modifiedName = transformMethodName(method.getName());

            return String.format(
                    """

                            #[test]
                            fn test_%s() {
                                let mut czc = test_helper::single_czc(%b);
                                let expected_datetime_strs = [
                            %s    ];

                                for ((loc, label), per_loc) in zip(
                                    zip(test_helper::more_locations(), test_helper::location_labels()),
                                    expected_datetime_strs,
                                ) {
                                    czc.set_geo_location(loc);
                                    for (date, expected) in zip(test_helper::sample_dates(), per_loc) {
                                        czc.set_date(date);
                                        let actual = czc.%s().map_or_else(
                                            || String::from("None"),
                                            |dt| dt.strftime("%s").to_string(),
                                        );
                                        assert_eq!(expected, actual, "at {label} on {date}");
                                    }
                                }
                            }
                            """,
                    modifiedName, useElevation, Helpers.nestedQuotedArrayBody(results, locs),
                    modifiedName, "%Y-%m-%d %H:%M:%S %z");
        } catch (Exception e) {
            return "\n// Could not invoke " + method.getName() + " because " + e.getMessage() + "\n";
        }
    }

    private static boolean isZmanGetter(Method method, StringBuilder content) {
        if (!method.getName().startsWith("get")) {
            content.append("\n// Skipped " + method.getName() + " because it isn't a getter\n");
            return false;
        }

        if (method.getParameterCount() != 0) {
            content.append("\n// Skipped " + method.getName() + " because it takes parameters\n");
            return false;
        }

        if (!Instant.class.equals(method.getReturnType())) {
            content.append("\n// Skipped " + method.getName() + " because it doesn't return an Instant\n");
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
            content.append(
                    "\n// Skipped " + method.getName() + " because it is one of the explicitly excluded methods\n");
            return false;
        }

        if (method.getName().contains("Mol") || method.getName().contains("Levana")) {
            content.append(
                    "\n// Skipped " + method.getName() + " because this library doesn't calculate molados (yet?)\n");
            return false;
        }

        if (method.getName().contains("Chametz")
                || method.getName().contains("TeshuvosVehanhagos")
                || method.getName().contains("Twilight")
                || method.getName().contains("Transit")) {
            content.append(
                    "\n// Skipped " + method.getName() + " because it contains a phrase which was explicitly excluded\n");
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
