package com.heal.dashboard.service.util;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public class DateUtil {
    public static String getTimeInGMT(long time) {
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat.format(time);
    }

    public static Date getDateInGMT(long time) throws ParseException {

        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateTime = simpleDateFormat.format(time);
        return simpleDateFormat.parse(dateTime);
    }
    public static String getLesserValue(String startTime, String endTime) {

        Timestamp st = null, et = null;
        try {
            st = getTimestampInGMT(startTime);
            et = getTimestampInGMT(endTime);
            return st.before(et) ? startTime : endTime;
        } catch (Exception e) {
            log.error("Error occurred while comparing timestamps.", e);
            return null;
        }
    }

    public static Timestamp getTimestampInGMT(String time) throws ParseException {

        DateFormat simpleDateFormat = null;
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        Date date = simpleDateFormat.parse(time.trim());
        java.sql.Timestamp timestamp = new java.sql.Timestamp(date.getTime());
        return timestamp;
    }

    public static String getGreaterValue(String startTime, String endTime) {

        Timestamp st = null, et = null;
        try {
            st = getTimestampInGMT(startTime);
            et = getTimestampInGMT(endTime);
            return st.after(et) ? startTime : endTime;
        } catch (Exception e) {
            log.error("Error occurred while comparing timestamps.", e);
            return null;
        }
    }

}