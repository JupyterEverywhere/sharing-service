package org.jupytereverywhere.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DateUtils {

    private DateUtils() {}

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String formatUTC(@NonNull Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static Timestamp utcDateToTimestamp(@NonNull Date date) {
        return new Timestamp(date.getTime());
    }
}
