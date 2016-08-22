package org.n52.wps.tamis;

import org.joda.time.DateTime;
import org.junit.Test;

public class EventTest {

    @Test
    public void testDateTimeBreaking(){
        
        DateTime dateTime = DateTime.parse("2016-07-01T00:15:00.000Z");
        
        Event event = new Event().setDateTime(dateTime);
        
        System.out.println(event.getDate());
        System.out.println(event.getTime());
        
    }
    
}
