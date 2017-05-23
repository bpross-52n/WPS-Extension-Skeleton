package org.n52.wps.tamis;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import nl.wldelft.fews.pi.EventComplexType;
import nl.wldelft.fews.pi.TimeSeriesComplexType;

public class TalsimToTimeseries {

    private static Logger LOGGER = LoggerFactory.getLogger(TalsimToTimeseries.class);

    public TalsimToTimeseries() {

    }

    public String createTimeseriesJSONFromTimeseriesXML(TimeSeriesComplexType seriesArray) throws IOException {

        // transform eventlist to jsonarray

        EventComplexType[] eventArray = seriesArray.getEventArray();

        StringWriter stringWriter = new StringWriter();

        JsonFactory f = new JsonFactory();
        JsonGenerator g = f.createGenerator(stringWriter);

        g.writeStartObject();

        g.writeArrayFieldStart("values");

        for (EventComplexType eventComplexType : eventArray) {

            double value = eventComplexType.getValue();

            Calendar date = eventComplexType.getDate();

            Calendar time = eventComplexType.getTime();
            String timestamp = date + "T" + time + "Z";
            g.writeStartObject();
            g.writeStringField("timestamp", TimeUtils.convertToUNIXTime(timestamp));
            g.writeStringField("value", value + "");
            g.writeEndObject();
        }

        g.writeEndArray();

        g.writeEndObject();
        g.close();
        
        return stringWriter.toString();
    }

}
