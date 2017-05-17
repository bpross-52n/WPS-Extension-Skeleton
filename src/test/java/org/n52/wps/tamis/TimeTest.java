package org.n52.wps.tamis;

import java.time.Instant;

import org.junit.Test;

public class TimeTest {

    @Test
    public void testTimeUtils(){
        
        String date = "2014-02-10";
        
        String time = "00:00:00";
        
        Instant dateTime  = TimeUtils.getInstant(date, time);
        
        System.out.println(dateTime.toString());
        
    }
    
}
