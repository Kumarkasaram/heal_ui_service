package com.heal.dashboard.service.util;

import com.appnomic.appsone.api.common.Constants;
import com.appnomic.appsone.api.pojo.AggregationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * @author Jitendra Kumar : 17/1/19
 */
public class DateTimeUtil {
    private static final Logger logger = LoggerFactory.getLogger(DateTimeUtil.class);
    private static final int cassandraDataOffset = Integer.parseInt(ConfProperties.getString(Constants.CASSANDRA_AGGREGATION_OFFSET,
            Constants.CASSANDRA_AGGREGATION_OFFSET_DEFAULT));
    private static String timezoneOffSet = ConfProperties.getString(Constants.TIMEZONE_OFFSET, Constants.TIMEZONE_OFFSET_DEFAULT);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Private constructor to prevent Instance creation
     */
    private DateTimeUtil() {
    }

    public static long getTimeInMilis(int timeInMinutes) {
        return TimeUnit.MINUTES.toMillis(timeInMinutes);
    }

    public static String getTime(String timeInMilis) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(StringUtils.getLong(timeInMilis));
    }

    public static String getTimeInGMT(long time) {

        DateFormat simpleDateFormat = null;
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return simpleDateFormat.format(time);
    }

    public static Date getDateInGMT(long time) throws ParseException {

        DateFormat simpleDateFormat = null;
        if (simpleDateFormat == null) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        String dateTime = simpleDateFormat.format(time);
        return simpleDateFormat.parse(dateTime);
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

    public static Timestamp getTimeStampWithOffset(String time, Long offset) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:00+0000");
        LocalDateTime localDateTime = LocalDateTime.parse(time, dateTimeFormatter);
        localDateTime = localDateTime.minus(offset, ChronoUnit.MILLIS);
        Timestamp timestamp = Timestamp.valueOf(localDateTime);
        return timestamp;
    }

    public static Timestamp getTimeStampWithoutOffset(Long time) {
        return new Timestamp(time);
    }

    public static Long getGMTToEpochTime(String time) {
        DateFormat simpleDateFormat = null;
        try {
            if (simpleDateFormat == null) {
                simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            }
            Date date = simpleDateFormat.parse(time.trim());
            return date.getTime();
        } catch (Exception e) {
            return 0l;
        }
    }

    public static String getEpochToGMTTime(Long epochTimestamp) {
        Date date = new Date(epochTimestamp);
        DateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:00");
        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return format.format(date);
    }

    public static LocalDateTime dateStrToLocalDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            logger.error("Exception While Parsing date", e);
        }
        return null;
    }

    public static List<LocalDateTime> getGeneratedTimeSeries(LocalDateTime localDateTime, long endTime, AggregationLevel aggregationLevel) {
        List<LocalDateTime> result = new ArrayList<>();
        //LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(roundedStartTime), ZoneId.of("UTC"));
        LocalDateTime localDateTimeEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("UTC"));
        result.add(localDateTime.plusMinutes(cassandraDataOffset));
        for (int i = 1; i <= aggregationLevel.getNoOfPoints(); i++) {
            if (aggregationLevel.getAggregrationValue() == AggregationLevel.YEARLY.getAggregrationValue())
                result.add(localDateTime.plusYears((long) i).plusMinutes(cassandraDataOffset));
            else if (aggregationLevel.getAggregrationValue() == AggregationLevel.MONTHLY.getAggregrationValue())
                result.add(localDateTime.plusMonths((long) i).plusMinutes(cassandraDataOffset));
            else if (aggregationLevel.getAggregrationValue() == AggregationLevel.DAILY.getAggregrationValue())
                result.add(localDateTime.plusDays((long) i).plusMinutes(cassandraDataOffset));
            else if (aggregationLevel.getAggregrationValue() == AggregationLevel.HOURLY.getAggregrationValue())
                result.add(localDateTime.plusHours((long) i).plusMinutes(cassandraDataOffset));
            else
                result.add(localDateTime.plusMinutes((long) i));

            if (!result.get(result.size() - 1).isBefore(localDateTimeEndTime)) {
                return result;
            }
        }
        return result;
    }

    public static List<LocalDateTime> getGeneratedTimeSeries(long startTime, long endTime, AggregationLevel aggregationLevel) {
        List<LocalDateTime> result = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("UTC"));
        LocalDateTime localDateTimeEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("UTC"));
        result.add(localDateTime.plusMinutes(cassandraDataOffset));
        for (int i = 1; i <= aggregationLevel.getNoOfPoints(); i++) {
            if (aggregationLevel.getAggregrationValue() == AggregationLevel.YEARLY.getAggregrationValue())
                result.add(localDateTime.plusYears((long) i).plusMinutes(cassandraDataOffset));
            else if (aggregationLevel.getAggregrationValue() == AggregationLevel.MONTHLY.getAggregrationValue())
                result.add(localDateTime.plusMonths((long) i).plusMinutes(cassandraDataOffset));
            else if (aggregationLevel.getAggregrationValue() == AggregationLevel.DAILY.getAggregrationValue())
                result.add(localDateTime.plusDays((long) i).plusMinutes(cassandraDataOffset));
            else if (aggregationLevel.getAggregrationValue() == AggregationLevel.HOURLY.getAggregrationValue())
                result.add(localDateTime.plusHours((long) i).plusMinutes(cassandraDataOffset));
            else
                result.add(localDateTime.plusMinutes((long) i));

            if (!result.get(result.size() - 1).isBefore(localDateTimeEndTime)) {
                return result;
            }
        }
        return result;
    }

    public static List<LocalDateTime> getGeneratedTimeSeriesWIP(LocalDateTime startTime, long endTime,
                                                                AggregationLevel aggregationLevel) {
        List<LocalDateTime> result = new ArrayList<>();
        try {
            LocalDateTime localDateTime = startTime;
            //LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("UTC"));
            LocalDateTime localDateTimeEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("UTC"));
            //result.add(localDateTime.plusMinutes(cassandraDataOffset));

            for (int i = 1; i <= aggregationLevel.getNoOfPoints(); i++) {

                if (aggregationLevel.getIncrementScale().equals("y"))
                    result.add(localDateTime.plusYears((long) (i * aggregationLevel.getIncrementValue())));

                else if (aggregationLevel.getIncrementScale().equals("mo"))
                    result.add(localDateTime.plusMonths((long) (i * aggregationLevel.getIncrementValue())));

                else if (aggregationLevel.getIncrementScale().equals("d"))
                    result.add(localDateTime.plusDays((long) (i * aggregationLevel.getIncrementValue())));

                else if (aggregationLevel.getIncrementScale().equals("h"))
                    result.add(localDateTime.plusHours((long) (i * aggregationLevel.getIncrementValue())));

                else
                    result.add(localDateTime.plusMinutes((long) (i * aggregationLevel.getIncrementValue())));

                //Adding 1 second so that if end time aligns with generated times' last point, it should not be removed.
                // Incase the last generated time point aligns perfectly with the end time point.
                if (!result.get(result.size() - 1).isBefore(localDateTimeEndTime/*.plusSeconds(1)*/)) {
                    //if the generated time has exceeded the "to-time", then we have to remove the invalid time interval added
                    //at the last
                    result.remove(result.size() - 1);
                    return result;
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while generating time series for from: {}, to: {}", startTime, endTime, e);
        }
        return result;
    }

    public static List<LocalDateTime> getGeneratedTimeSeriesWIP(long startTime, long endTime,
                                                                AggregationLevel aggregationLevel, long timzoneOffset) {
        List<LocalDateTime> result = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("UTC"));//LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("UTC"));
        LocalDateTime localDateTimeEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("UTC"));
        //result.add(localDateTime.plusMinutes(cassandraDataOffset));
        for (int i = 1; i <= aggregationLevel.getNoOfPoints(); i++) {

            if (aggregationLevel.getIncrementScale().equals("y"))
                result.add(localDateTime.plusYears((long) (i * aggregationLevel.getIncrementValue())).
                        plusMinutes(timzoneOffset));
            else if (aggregationLevel.getIncrementScale().equals("mo"))
                result.add(localDateTime.plusMonths((long) (i * aggregationLevel.getIncrementValue())).
                        plusMinutes(timzoneOffset));
            else if (aggregationLevel.getIncrementScale().equals("d"))
                result.add(localDateTime.plusDays((long) (i * aggregationLevel.getIncrementValue())).
                        plusMinutes(timzoneOffset));
            else if (aggregationLevel.getIncrementScale().equals("h"))
                result.add(localDateTime.plusHours((long) (i * aggregationLevel.getIncrementValue())).
                        plusMinutes(timzoneOffset));
            else
                result.add(localDateTime.plusMinutes((long) (i * aggregationLevel.getIncrementValue())));

            //Adding 1 second so that if end time aligns with generated times' last point it should not be removed, incase
            //the last generated time point aligns perfectly with the end time point.
            if (!result.get(result.size() - 1).isBefore(localDateTimeEndTime.plusSeconds(1))) {
                //if the generated time has exceeded the "to-time", then we have to remove the invalid time interval added
                //at the last
                result.remove(result.size() - 1);
                return result;
            }
        }
        return result;
    }

    /**
     * @param startTime
     * @param endTime
     * @param aggregationLevel
     * @return
     */
    public static List<Long> getCustomGeneratedTimeSeries(long startTime, long endTime, AggregationLevel aggregationLevel) {

        List<Long> result = new ArrayList<>();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.of("UTC"));
        LocalDateTime localDateTimeEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("UTC"));
        result.add(localDateTime.plusMinutes(cassandraDataOffset).atZone(ZoneId.of("UTC")).toEpochSecond() * 1000);

        for (int i = 1; i <= aggregationLevel.getNoOfPoints(); i++) {

            if (aggregationLevel.getIncrementScale().equals("m")) {
                result.add(localDateTime.plusMinutes((long) i * aggregationLevel.getIncrementValue()).atZone(ZoneId.of("UTC")).toEpochSecond() * 1000);
            } else if (aggregationLevel.getIncrementScale().equals("h")) {
                result.add(localDateTime.plusHours((long) i * aggregationLevel.getIncrementValue()).atZone(ZoneId.of("UTC")).toEpochSecond() * 1000);
            } else if (aggregationLevel.getIncrementScale().equals("d")) {
                result.add(localDateTime.plusDays((long) i * aggregationLevel.getIncrementValue()).atZone(ZoneId.of("UTC")).toEpochSecond() * 1000);
            } else if (aggregationLevel.getIncrementScale().equals("mo")) {
                result.add(localDateTime.plusMonths((long) i * aggregationLevel.getIncrementValue()).atZone(ZoneId.of("UTC")).toEpochSecond() * 1000);
            } else if (aggregationLevel.getIncrementScale().equals("y")) {
                result.add(localDateTime.plusYears((long) i * aggregationLevel.getIncrementValue()).atZone(ZoneId.of("UTC")).toEpochSecond() * 1000);
            }

            if (result.get(result.size() - 1) > (localDateTimeEndTime.atZone(ZoneId.of("UTC")).toEpochSecond() * 1000)) {
                return result;
            }
        }
        return result;
    }

    public static List<LocalDateTime> getGeneratedTimeSeriesMLWB(LocalDateTime localDateTime, long endTime, AggregationLevel aggregationLevel) {
        List<LocalDateTime> result = new ArrayList<>();
        //LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(roundedStartTime), ZoneId.of("UTC"));
        LocalDateTime localDateTimeEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.of("UTC"));
        long i = 1;
        result.add(localDateTime);
        while (result.get(result.size() - 1).isBefore(localDateTimeEndTime)) {
            result.add(localDateTime.plusMinutes((long) i));
            i++;
        }
        return result;
    }

    public static LocalDateTime getStartTimeStamp(long startTimeEpoch, AggregationLevel aggregationLevel) {

        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeEpoch), ZoneId.of("UTC"));
        if (aggregationLevel.getAggregrationValue() == AggregationLevel.YEARLY.getAggregrationValue()) {
            startTime = startTime.withMonth(0);
        }
        if (aggregationLevel.getAggregrationValue() >= AggregationLevel.MONTHLY.getAggregrationValue()) {
            startTime = startTime.withDayOfMonth(1);
        }
        if (aggregationLevel.getAggregrationValue() >= AggregationLevel.DAILY.getAggregrationValue()) {
            startTime = startTime.withHour(0);
        }
        if (aggregationLevel.getAggregrationValue() >= AggregationLevel.HOURLY.getAggregrationValue()) {
            startTime = startTime.withMinute(0);
        }
        if (aggregationLevel.getAggregrationValue() >= AggregationLevel.MINUTELY.getAggregrationValue()) {
            startTime = startTime.withSecond(0);
        }
        startTime = startTime.withNano(0);
        return startTime;
    }

    public static LocalDateTime getStartTimeStampWIP(long startTimeEpoch, AggregationLevel aggregationLevel,
                                                     long offset) {

        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeEpoch), ZoneId.of("UTC"));

        if (aggregationLevel.getAggregrationValue() == 525600) {
            startTime = startTime.withMonth(0);
        }

        if (aggregationLevel.getAggregrationValue() >= 44640) {
            startTime = startTime.withDayOfMonth(1);
        }

        if (aggregationLevel.getAggregrationValue() >= 1440) {
            startTime = startTime.withHour(0);
        }

        if (aggregationLevel.getAggregrationValue() >= 60) {
            startTime = startTime.withMinute(0);
        }

        if (aggregationLevel.getAggregrationValue() >= 1) {
            if (aggregationLevel.getAggregrationValue() == 30) {

                startTime = startTime.withSecond(0);
                for (int i = 0; i < 30; i++) {
                    long minute = startTime.getMinute();
                    if (minute == 0 || minute == 30)
                        break;
                    //We subtract delta until it matches a whole value of 30
                    startTime = startTime.minusMinutes(1);
                }

            } else if (aggregationLevel.getAggregrationValue() == 15) {

                startTime = startTime.withSecond(0);
                for (int i = 0; i < 15; i++) {
                    long minute = startTime.getMinute();
                    if (minute == 0 || minute == 15 || minute == 30 || minute == 45)
                        break;
                    //We subtract delta until it matches a whole value of 15
                    startTime = startTime.minusMinutes(1);
                }

            } else {

                startTime = startTime.withSecond(0);

            }
        }

        startTime = startTime.withNano(0);
        //apply offset based on aggregation level
        startTime = addOffset(startTimeEpoch, startTime, offset, aggregationLevel);

        return startTime;
    }

    public static LocalDateTime addOffset(long startTimeEpoch, LocalDateTime startTime, long offset,
                                          AggregationLevel aggregationLevel) {
        LocalDateTime baseStartTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeEpoch), ZoneId.of("UTC"));
        //apply offset based on aggregation level
        if (aggregationLevel.getAggregrationValue() > 60) {
            startTime = startTime.minusSeconds((offset / 1000));
        } else if (aggregationLevel.getAggregrationValue() == 60) {
            //extract only minutes from offset value, because only minutes is offset when agg level is hourly
            long minutes = ((offset % (60 * 60 * 1000)) / (60 * 1000));
            startTime = startTime.minusMinutes(minutes);
        }

        //check that adding offset to has not shifted the point far into the past than required, we will remove all
        // generated that are before the from time provided by the user, ideally we need just 1 aggregated point before
        // the user provided from time.
        while (startTime.isBefore(baseStartTime)) {
            startTime = startTime.plusMinutes(aggregationLevel.getAggregrationValue());
        }

        //subtracting 1 minute as the time series generating function will start generation from input parameter(exclusive)
        return startTime.minusMinutes(aggregationLevel.getAggregrationValue());
    }

    /**
     * It will check the difference between timestamps and return the required aggregation level
     *
     * @param fromTime
     * @param toTime
     * @return
     */
    public static AggregationLevel getAggregationLevel(long fromTime, long toTime) {
        long differenceInMinutes = (toTime - fromTime) / (1000 * 60);

        if (differenceInMinutes >= AggregationLevel.YEARLY.getAggregrationValue()) {
            return AggregationLevel.YEARLY;
        } else if (differenceInMinutes >= AggregationLevel.MONTHLY.getAggregrationValue()) {
            return AggregationLevel.MONTHLY;
        } else if (differenceInMinutes > AggregationLevel.DAILY.getAggregrationValue()) {
            return AggregationLevel.DAILY;
        } else if (differenceInMinutes > AggregationLevel.HOURLY.getAggregrationValue()) {
            return AggregationLevel.HOURLY;
        } else {
            return AggregationLevel.MINUTELY;
        }

    }

    public static AggregationLevel getAggregationLevelWIP(long fromTime, long toTime) {
        long differenceInMinutes = (toTime - fromTime) / (1000 * 60);

        if (differenceInMinutes >= AggregationLevel.YEARLY.getAggregrationValue()) {

            return AggregationLevel.YEARLY;

        } else if ((differenceInMinutes > AggregationLevel.MONTHLY.getAggregrationValue())) {

            return AggregationLevel.MONTHLY;

        } else if (differenceInMinutes > (AggregationLevel.DAILY_WEEK.getAggregrationValue() * AggregationLevel.DAILY_WEEK.getNoOfPoints())) {

            return AggregationLevel.DAILY_MONTH;

        } else if (differenceInMinutes > AggregationLevel.DAILY_WEEK.getAggregrationValue()) {

            return AggregationLevel.DAILY_WEEK;

        } else if (differenceInMinutes > (AggregationLevel.THIRTYMINUTELY_TWELVEHOUR.getAggregrationValue() *
                AggregationLevel.THIRTYMINUTELY_TWELVEHOUR.getNoOfPoints())) {

            return AggregationLevel.HOURLY_TWENTYHOUR;

        } else if (differenceInMinutes > (AggregationLevel.FIFTEENMINUTELY_FOURHOUR.getAggregrationValue() *
                AggregationLevel.FIFTEENMINUTELY_FOURHOUR.getNoOfPoints())) {

            return AggregationLevel.THIRTYMINUTELY_TWELVEHOUR;

        } else if (differenceInMinutes > (AggregationLevel.MINUTELY_FULLHOUR.getAggregrationValue() *
                AggregationLevel.MINUTELY_FULLHOUR.getNoOfPoints())) {

            return AggregationLevel.FIFTEENMINUTELY_FOURHOUR;

        } else if (differenceInMinutes > (AggregationLevel.MINUTELY_HALFHOUR.getAggregrationValue() *
                AggregationLevel.MINUTELY_HALFHOUR.getNoOfPoints())) {

            return AggregationLevel.MINUTELY_FULLHOUR;

        } else if (differenceInMinutes > AggregationLevel.MINUTELY_FULLHOUR.getAggregrationValue()) {

            return AggregationLevel.MINUTELY_HALFHOUR;

        } else {

            return AggregationLevel.INVALID_RANGE;
        }

    }

    public static AggregationLevel getAggregationLevelForTopN(long fromTime, long toTime) {
        long differenceInMinutes = (toTime - fromTime) / (1000 * 60);

        if (differenceInMinutes >= AggregationLevel.YEARLY.getAggregrationValue()) {
            return AggregationLevel.YEARLY;
        } else if (differenceInMinutes >= AggregationLevel.MONTHLY.getAggregrationValue()) {
            return AggregationLevel.YEARLY;
        } else if (differenceInMinutes > AggregationLevel.DAILY.getAggregrationValue()) {
            return AggregationLevel.MONTHLY;
        } else if (differenceInMinutes > AggregationLevel.HOURLY.getAggregrationValue()) {
            return AggregationLevel.DAILY;
        } else {
            return AggregationLevel.HOURLY;
        }

    }

    public static String getGMTTimeFromCassandraforProblemDetail(String timeFromCassandra, long timeOffSet) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
        DateTimeFormatter dateTimeFormatterSend = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
        LocalDateTime localDateTime = LocalDateTime.parse(timeFromCassandra, dateTimeFormatter);
        localDateTime = localDateTime.minus(timeOffSet, ChronoUnit.MILLIS);
        return localDateTime.format(dateTimeFormatterSend);
    }

    public static String getGMTTimeFromCassandra(String timeFromCassandra, long timeOffSet) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu");
        DateTimeFormatter dateTimeFormatterSend = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
        LocalDateTime localDateTime = LocalDateTime.parse(timeFromCassandra, dateTimeFormatter);
        localDateTime = localDateTime.minus(timeOffSet, ChronoUnit.MILLIS);
        return localDateTime.format(dateTimeFormatterSend);
    }

    public static long getEPOCHGMTTimeFromCassandra(String timeFromCassandra, long timeOffSet) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu");
        DateTimeFormatter dateTimeFormatterSend = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
        LocalDateTime localDateTime = LocalDateTime.parse(timeFromCassandra, dateTimeFormatter);
        localDateTime = localDateTime.minus(timeOffSet, ChronoUnit.MILLIS);
        return getGMTToEpochTime(localDateTime.format(dateTimeFormatterSend));
    }

    public static String getGMTTimeFromCassandra(LocalDateTime time, long timeOffset) {
        DateTimeFormatter dateTimeFormatterSend = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:00");
        time = time.minus(timeOffset, ChronoUnit.MILLIS);
        return time.format(dateTimeFormatterSend);
    }

    public static long getCassandraTimeGMT(long epochTime) {
        return (epochTime + Long.valueOf(timezoneOffSet));
    }

    public static String getGreaterValue(String startTime, String endTime) {

        Timestamp st = null, et = null;
        try {
            st = getTimestampInGMT(startTime);
            et = getTimestampInGMT(endTime);
            return st.after(et) ? startTime : endTime;
        } catch (Exception e) {
            logger.error("Error occurred while comparing timestamps.", e);
            return null;
        }
    }

    public static String getLesserValue(String startTime, String endTime) {

        Timestamp st = null, et = null;
        try {
            st = getTimestampInGMT(startTime);
            et = getTimestampInGMT(endTime);
            return st.before(et) ? startTime : endTime;
        } catch (Exception e) {
            logger.error("Error occurred while comparing timestamps.", e);
            return null;
        }
    }

    public static LocalDateTime epochSecondsToLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    public static int getTotalDaysInYear(int year) {
        if ((year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0)))
            return 366;
        else
            return 365;

    }

    public static int getDaysInMonth(int month, int year) {

        int numberOfDaysInMonth = 0;
        switch (month) {
            case 1:

                numberOfDaysInMonth = 31;
                break;
            case 2:

                if ((year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0))) {
                    numberOfDaysInMonth = 29;
                } else {
                    numberOfDaysInMonth = 28;
                }
                break;
            case 3:

                numberOfDaysInMonth = 31;
                break;
            case 4:
                numberOfDaysInMonth = 30;
                break;
            case 5:
                numberOfDaysInMonth = 31;
                break;
            case 6:
                numberOfDaysInMonth = 30;
                break;
            case 7:
                numberOfDaysInMonth = 31;
                break;
            case 8:
                numberOfDaysInMonth = 31;
                break;
            case 9:
                numberOfDaysInMonth = 30;
                break;
            case 10:
                numberOfDaysInMonth = 31;
                break;
            case 11:
                numberOfDaysInMonth = 30;
                break;
            case 12:
                numberOfDaysInMonth = 31;
                break;
            default:
                numberOfDaysInMonth = 30;
        }
        return numberOfDaysInMonth;
    }

    public static long getTimeBasedOnAggLevel(long epochSecond, AggregationLevel aggregationLevel) {
        long tempEpoch = 1;
        switch (aggregationLevel) {
            case MINUTELY:
            case QUARTERHOURLY:
            case MINUTELY_HALFHOUR:
            case MINUTELY_FULLHOUR:
                tempEpoch = epochSecond / 60;
                epochSecond = tempEpoch * 60;
                break;
            case FIFTEENMINUTELY_FOURHOUR:
                tempEpoch = epochSecond / 900;
                epochSecond = tempEpoch * 900;
                break;
            case THIRTYMINUTELY_TWELVEHOUR:
                tempEpoch = epochSecond / 1800;
                epochSecond = tempEpoch * 1800;
                break;

            case HOURLY:
            case HOURLY_TWENTYHOUR:
            case SIXHOURLY:
                tempEpoch = epochSecond / 3600;
                epochSecond = tempEpoch * 3600;
                break;
            case DAILY_MONTH:
            case DAILY_WEEK:
            case DAILY:
                tempEpoch = epochSecond / 86400;
                epochSecond = tempEpoch * 86400;
                break;
            case MONTHLY:
                LocalDateTime epochLocalDateTime = DateTimeUtil.epochSecondsToLocalDateTime(epochSecond);
                long daysInMonths = (long) DateTimeUtil.getDaysInMonth(epochLocalDateTime.getMonthValue(), epochLocalDateTime.getYear());
                epochSecond = epochSecond / (daysInMonths * 86400);
                break;
            case YEARLY:
                long daysInYear = (long) DateTimeUtil.getTotalDaysInYear(DateTimeUtil.epochSecondsToLocalDateTime(epochSecond).getYear());
                epochSecond = epochSecond / (daysInYear * 86400);
                break;
            default:
                break;

        }
        return epochSecond;
    }

    public static Timestamp getCurrentTimestampInGMT() {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.DATE_TIME);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat localDateFormat = new SimpleDateFormat(Constants.DATE_TIME);
            return new Timestamp(localDateFormat.parse( simpleDateFormat.format(new Date())).getTime());
        } catch (ParseException e) {
            logger.error("Error in getting current time stamp in GMT", e);
        }
        return null;
    }
}
