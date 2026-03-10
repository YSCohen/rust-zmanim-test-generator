package io.github.YSCohen.rustZmanimTestGenerator;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.Date;

import com.kosherjava.zmanim.util.GeoLocation;

public class Helpers {
        protected static GeoLocation[] getLocs() {
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
        GeoLocation fiji = new GeoLocation("FJ", -17.633056, 178.016667, 1324,
                TimeZone.getTimeZone("Pacific/Fiji"));
        GeoLocation honolulu = new GeoLocation("HU", 21.466667, -157.966667, 10,
                TimeZone.getTimeZone("America/Adak"));
        GeoLocation niue = new GeoLocation("NI", -19.053006, -169.859199, 75,
                TimeZone.getTimeZone("Pacific/Niue"));
        GeoLocation[] locs = { lakewood, jerusalem, los_angeles, tokyo, arctic_nunavut, samoa, fiji, honolulu, niue };
        return locs;
    }

    protected static String formatDate(Date date, TimeZone timeZone) {
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
}
