package com.heal.dashboard.service.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.heal.dashboard.service.beans.util.RollupTimeMetaData;

public class CassandraRollupUtility {
	private static final int[] aggLevels = CommonUtils.getAggregationLevels();
	private List<RollupTimeMetaData> timeRanges = new ArrayList<>();
	public void process(long fromTime, long toTime, long timezoneOffset) {

        int aggLevel = getAgglevel(fromTime, toTime);
        long incrementValue = (long) aggLevel * ( 1000 * 60 );
        long generatedStartTimepoint = computeStartForAggLevel(fromTime, timezoneOffset, aggLevel);
        RollupTimeMetaData currentRollup = new RollupTimeMetaData();
        if( fromTime < toTime ) {
            currentRollup = getTimeRange(fromTime, toTime, generatedStartTimepoint, aggLevel);
        }
        long curFromTime = currentRollup.getFrom();
        long curToTime = currentRollup.getTo();
        if( (curToTime + incrementValue) != toTime) {
            //The last point needs manual collation hence remove that
            currentRollup.setTo(curToTime - incrementValue);
        }
        timeRanges.add(currentRollup);

        if( curFromTime > fromTime ) {
            // left spill over exists
            process(fromTime, curFromTime, timezoneOffset);
        }

        if( curToTime < toTime && (curToTime + incrementValue) != toTime ) {
            //a check in-case there is only one aggregated point and still spill over exists
            if( (curToTime+incrementValue) < toTime) {
                process(curToTime+incrementValue, toTime, timezoneOffset);
            } else {
                //right spill exists
                process(curToTime, toTime, timezoneOffset);
            }
        }

    }

	  private int getAgglevel(long fromTime, long toTime) {
	        long diff = (toTime - fromTime);
	        int diffInMinutes = (int) (diff / ( 1000 * 60 ));

	        for(int i=0; i<aggLevels.length - 1; i++) {
	            if( diffInMinutes >= ( 2 * aggLevels[i])) {
	                return aggLevels[i];
	            }
	        }
	        //If time difference is 15 minutes and it is complete window then return agg level = 15 , for optimised queries.
	        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromTime), ZoneId.of("UTC"));
	        long minute = startTime.getMinute();
	        if( ( minute == 15 || minute == 30 || minute == 45 || minute == 0) && diffInMinutes >= 15 ) {
	            return 15;
	        }

	        //If none match then return lowest agg level
	        return aggLevels[aggLevels.length-1];
	    }
	  
	  private RollupTimeMetaData getTimeRange(long fromTime, long toTime, long startTimePoint, int aggLevel) {
	        RollupTimeMetaData result = new RollupTimeMetaData();
	        result.setAggLevel(aggLevel);
	        long incrementSize = aggLevel * ( 1000L * 60L );

	        while ( startTimePoint < toTime ) {

	            if( result.getFrom() == 0L  && startTimePoint >= fromTime ) {
	                result.setFrom(startTimePoint);
	            }

	            startTimePoint += incrementSize;
	        }

	        result.setTo(startTimePoint-incrementSize);
	        return result;
	    }
	  
	  
	  private long computeStartForAggLevel(long fromTime, long timezoneOffset, int aggLevel) {
	        LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromTime), ZoneId.of("UTC"));

	        startTime = startTime.withSecond(0);
	        if( aggLevel >= 1440 ) { startTime = startTime.withHour(0); }
	        if( aggLevel >= 60 ) { startTime = startTime.withMinute(0); }

	        if ( aggLevel == 30 ) {
	            for (int i=0;i<29;i++) {
	                long minute = startTime.getMinute();
	                if (minute == 0 || minute == 30) break;
	                //If we want agg level of 30 then start time then we need to round to any 00,30
	                startTime = startTime.minusMinutes(1);
	            }
	        } else if( aggLevel == 15 ) {
	            for (int i=0;i<14;i++) {
	                long minute = startTime.getMinute();
	                if (minute == 0 || minute == 15 || minute == 30 || minute == 45) break;
	                //If we want agg level of 15 then start time then we need to round to any 00,15,30,45
	                startTime = startTime.minusMinutes(1);
	            }
	        }

	        if ( aggLevel > 60 && timezoneOffset != 0L ) {
	            startTime = startTime.minusSeconds((timezoneOffset / 1000));
	        } else if ( aggLevel == 60 && timezoneOffset != 0L ) {
	            //extract only minutes from offset value, because only minutes is offset when agg level is hourly
	            long minutes = ((timezoneOffset % (60 * 60 * 1000)) / (60 * 1000));
	            startTime = startTime.minusMinutes(minutes);
	        }

	        return startTime.atZone(ZoneId.of("UTC")).toEpochSecond() * 1000;
	    }
	
	  public List<RollupTimeMetaData> getTimeRanges() {
	        return this.timeRanges;
	    }
	  
}
