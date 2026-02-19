package io.github.YSCohen.rustZmanimTestGenerator;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;

import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.ComprehensiveZmanimCalendar;

import com.google.common.base.CaseFormat;

public class App {
    public static void main(String[] args) {
        boolean elev = (args.length > 0 && args[0].equals("elev"));
        generateAllZmanTests(elev);
    }

    private static void generateAllZmanTests(boolean useElevation) {
        GeoLocation[] locs = getLocs();

        ComprehensiveZmanimCalendar czc = new ComprehensiveZmanimCalendar();
        Method[] methods = czc.getClass().getMethods();

        System.out.println("mod test_helper;\nuse std::iter::zip;");

        for (Method method : methods) {
            if (isZmanGetter(method)) {
                generateSingleZmanTest(locs, method, useElevation);
            }
        }
    }

    private static void generateSingleZmanTest(GeoLocation[] locs, Method method, boolean useElevation) {
        try {
            String[] results = new String[6];
            for (int i = 0; i < 6; i++) {
                Calendar cal = Calendar.getInstance(locs[i].getTimeZone());
                cal.set(2017, Calendar.OCTOBER, 17, 0, 0, 0);
                ComprehensiveZmanimCalendar czcOfLocation = new ComprehensiveZmanimCalendar(locs[i]);
                czcOfLocation.setCalendar(cal);
                czcOfLocation.setUseElevation(useElevation);

                Date value = (Date) method.invoke(czcOfLocation);
                results[i] = iso(value, locs[i].getTimeZone());
            }

            String modifiedName = transformMethodName(method.getName());

            System.out.printf(
                    """

                            #[test]
                            fn test_%s() {
                                let cals = test_helper::basic_location_czcs(%b);
                                let expected_datetime_strs = [
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                    "%s",
                                ];

                                for (czc, edt) in zip(cals, expected_datetime_strs) {
                                    let result = match czc.%s() {
                                        Some(dt) => dt.format("%s").to_string(),
                                        None => String::from("None"),
                                    };
                                    assert_eq!(result, edt)
                                }
                            }
                            """,
                    modifiedName, useElevation, results[0], results[1],
                    results[2], results[3], results[4],
                    results[5].replace("GMT+14:00", "+14").replace("WSDT", "+14"),
                    modifiedName, "%Y-%m-%d %H:%M:%S %Z");
        } catch (Exception e) {
            System.out.println("\n// Could not invoke " + method.getName());
        }
    }

    private static GeoLocation[] getLocs() {
        GeoLocation lakewood = new GeoLocation("LW", 40.0721087, -74.2400243, 15,
                TimeZone.getTimeZone("America/New_York"));
        GeoLocation samoa = new GeoLocation("SM", -13.8599098, -171.8031745, 1858,
                TimeZone.getTimeZone("Pacific/Apia"));
        GeoLocation jerusalem = new GeoLocation("JM", 31.7781161, 35.233804, 740.0,
                TimeZone.getTimeZone("Asia/Jerusalem"));
        GeoLocation los_angeles = new GeoLocation("LA", 34.0201613, -118.6919095, 71,
                TimeZone.getTimeZone("America/Los_Angeles"));
        GeoLocation tokyo = new GeoLocation("TK", 35.6733227, 139.6403486, 40,
                TimeZone.getTimeZone("Asia/Tokyo"));
        GeoLocation arctic_nunavut = new GeoLocation("AN", 81.7449398, -64.7945858, 127,
                TimeZone.getTimeZone("America/Toronto"));
        GeoLocation[] locs = { lakewood, jerusalem, los_angeles, tokyo, arctic_nunavut, samoa };
        return locs;
    }

    private static String iso(Date date, TimeZone timeZone) {
        if (date == null) {
            return "None";
        } else {
            Calendar cal = Calendar.getInstance(timeZone);
            cal.setTime(date);
            return String.format("%04d-%02d-%02d %02d:%02d:%02d %s",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND),
                    timeZone.getDisplayName(cal.getTimeZone().inDaylightTime(date), TimeZone.SHORT));
        }
    }

    private static boolean isZmanGetter(Method method) {
        return method.getName().startsWith("get")
                && method.getParameterCount() == 0
                && Date.class.equals(method.getReturnType())
                && !method.getName().equals("getClass")
                && !method.getName().equals("getSunset")
                && !method.getName().equals("getSunrise")
                && !method.getName().equals("getSeaLevelSunset")
                && !method.getName().equals("getSeaLevelSunrise")
                && !method.getName().equals("getAlosHashachar")
                && !method.getName().equals("getTzais")
                && !method.getName().equals("getMinchaGedola")
                && !method.getName().equals("getMinchaKetana")
                && !method.getName().equals("getPlagHamincha")
                && !method.getName().equals("getSofZmanShmaMGA")
                && !method.getName().equals("getCandleLighting")
                && !method.getName().equals("getSofZmanTfilaMGA")
                && !method.getName().equals("getChatzosAsHalfDay")
                && !method.getName().contains("Mol")
                && !method.getName().contains("Levana")
                && !method.getName().contains("Chametz")
                && !method.getName().contains("Twilight")
                && !method.getName().contains("Transit");
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
                .replace("le_mincha", "lemincha")
                .replace("solar_midnight", "chatzos_halayla")
                .replace("plag_alos_to_sunset", "plag_alos_16_1_degrees_to_sunset")
                .replace("mincha_gedola_mga_30_minutes", "mincha_gedola_30_minutes")
                .replace("alos_16_1_to", "alos_16_1_degrees_to")
                .replace("greaterthan", "greater_than")
                .replace("gedola_greater", "gedola_gra_greater")
                .replace("hashemashos", "hashmashos");
    }
}
