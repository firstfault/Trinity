package me.f1nal.trinity.util;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;

public class TimeUtil {
    public static String getFormattedTime(long millis) {
        String pattern = "MM/dd/yyyy hh:mm a";
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(Date.from(Instant.ofEpochMilli(millis)));
    }
}
