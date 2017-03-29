package org.n52.wps.tamis;

import java.time.Instant;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeUtils {
    
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss");
    
    public static String convertToUNIXTime(String timeStamp){
        
        String result = "";
        
        Instant parsedDate = Instant.parse(timeStamp);
        
        result = parsedDate.toEpochMilli() + "";
        
        return result;
        
    }
    
    public static String convertFromUNIXTime(String timeStamp){
        
        String result = "";
        
        Instant parsedDate = Instant.ofEpochMilli(Long.valueOf(timeStamp));
        
        result = parsedDate.toString();
        
        return result;
        
    }

}
