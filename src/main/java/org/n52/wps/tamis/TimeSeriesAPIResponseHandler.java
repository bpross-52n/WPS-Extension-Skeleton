package org.n52.wps.tamis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TimeSeriesAPIResponseHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(TimeSeriesAPIResponseHandler.class);

    private InputStream inputStream;

    private String startDate;

    private String startTime;

    private String endDate;

    private String endTime;

    private String missVal;

    private String units;
    
    private String timeStepMultiplier;

    private FEWObject fewObject;

    private List<Event> eventList;

    private OutputStream outputStream;
        
    private ObjectMapper m;

    public TimeSeriesAPIResponseHandler() {
        eventList = new ArrayList<>();

        m = new ObjectMapper();
    }

    public void handle() {

        checkPrerequisites();
        
        //fix to one hour (3600 seconds)
        timeStepMultiplier = "3600";

        try {

            JsonNode rootNode = m.readTree(inputStream);
            
            JsonNode valuesNode = rootNode.path("values");

                if (valuesNode instanceof ArrayNode) {
                    
                    ArrayNode valuesArrayNode = (ArrayNode)valuesNode;
                    
                    LOGGER.info("Got JSON array of length {}.", valuesArrayNode.size());
                    
                    DateTime currentHourDT = null;
                    
                    Iterator<JsonNode> valuesIterator = valuesArrayNode.iterator();
                    
                    while (valuesIterator.hasNext()) {
                        JsonNode jsonNode = (JsonNode) valuesIterator.next();

                        JsonNode timeStampNode = jsonNode.path("timestamp");
                        JsonNode valueNode = jsonNode.path("value");
                       
                        String timeString = TimeUtils.convertFromUNIXTime(timeStampNode.asText());
                        
                        DateTime dateTime = DateTime.parse(timeString);
                        
                        DateTime fullHour = dateTime.hourOfDay().roundFloorCopy();
                        
                        if(startDate == null || startDate.isEmpty()){
                            startDate = fullHour.toString(TimeUtils.DATE_FORMATTER);
                            startTime = fullHour.toString(TimeUtils.TIME_FORMATTER);
                        }                        
                        
                        String value = valueNode.asText();
                        
                        if(currentHourDT != null){
                            
                            if( fullHour.isAfter(currentHourDT)){
                                
                                if(Hours.hoursBetween(currentHourDT, fullHour).getHours() > 1){
                                    
                                    fillUpGap(currentHourDT, fullHour, value);
                                    
                                }else {                                    
                                    eventList.add(new Event().setDateTime(fullHour).setValue(value));                                    
                                }
                                currentHourDT = fullHour;                                
                            }
                            
                        }else{                            
                            currentHourDT = fullHour;
                            
                            eventList.add(new Event().setDateTime(fullHour).setValue(value));                            
                        }
                        
                        if(!valuesIterator.hasNext()){
                            endDate = fullHour.toString(TimeUtils.DATE_FORMATTER);
                            endTime = fullHour.toString(TimeUtils.TIME_FORMATTER);                            
                        }
                    }
                }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

//        fewObject.setUnits(units);
        fewObject.setEventList(eventList);
        fewObject.setStartDate(startDate);
        fewObject.setStartTime(startTime);
        fewObject.setEndDate(endDate);
        fewObject.setEndTime(endTime);
        fewObject.setTimeStepMultiplier(timeStepMultiplier);

        Writer stream = new OutputStreamWriter(outputStream);

        new TimeSeriesToTalsim().setOutputStream(stream).setFEWObject(fewObject).createTimeSeries();

        try {
            stream.close();
        } catch (IOException e) {
            /* ignore */
        }
    }

    //fills up a gap between two dates with a constant value each hour
    public void fillUpGap(DateTime start, DateTime end, String value){
                
        start = start.hourOfDay().roundFloorCopy();
        end = end.hourOfDay().roundFloorCopy();
        
        Hours hours = Hours.hoursBetween(start, end);
                
        for (int i = 0; i < hours.getHours(); i++) {
            eventList.add(new Event().setDateTime(start.plusHours(i)).setValue(value));
        }
    }
    
    private void checkPrerequisites() {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream not set.");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("OutputStream not set.");
        }
    }

    public TimeSeriesAPIResponseHandler setInputStream(InputStream stream) {
        this.inputStream = stream;
        return this;
    }

    public TimeSeriesAPIResponseHandler setOutputStream(OutputStream stream) {
        this.outputStream = stream;
        return this;
    }

    public FEWObject getFEWObject() {
        return fewObject;
    }

    public TimeSeriesAPIResponseHandler setFEWObject(FEWObject fewObject) {
        this.fewObject = fewObject;
        return this;
    }

    public List<Event> getEventList() {
        return eventList;
    }

    public void setEventList(List<Event> eventList) {
        this.eventList = eventList;
    }
}
