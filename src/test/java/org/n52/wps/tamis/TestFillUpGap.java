package org.n52.wps.tamis;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

public class TestFillUpGap {

    @Test
    public void testFillUpGap(){
        
        DateTime now = DateTime.now();
        
        DateTime in36Hours = now.plusHours(36);
        
        TimeSeriesAPIResponseHandler apiResponseHandler = new TimeSeriesAPIResponseHandler();
        
        apiResponseHandler.fillUpGap(now, in36Hours, "0.7");
        
        List<Event> events = apiResponseHandler.getEventList();
        
        for (Event event : events) {
            System.out.println(event.getDateTime() + " " + event.getValue());
        }
        
    }
    
    
}
