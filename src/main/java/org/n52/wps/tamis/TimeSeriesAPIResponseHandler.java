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
    
    private String timeStepMultiplier;

    private FEWObject fewObject;

    private List<Event> eventList;

    private OutputStream outputStream;
        
    private ObjectMapper m;

    private ArrayNode valuesArrayNode;
    
    private DateTime startDateTime;
    
    private DateTime endDateTime;

    public TimeSeriesAPIResponseHandler() {
        eventList = new ArrayList<>();

        m = new ObjectMapper();
    }

    public TimeSeriesAPIResponseHandler prepareFEWObject() {

        checkPrerequisites();
        
        //fix to one hour (3600 seconds)
        //fix to quarter hour (900 seconds)
        //fix to half an hour (1800 seconds)
        timeStepMultiplier = "1800";

        try {

            JsonNode rootNode = m.readTree(inputStream);
            
            JsonNode valuesNode = rootNode.path("values");

                if (valuesNode instanceof ArrayNode) {
                    
                    valuesArrayNode = (ArrayNode)valuesNode;
                    
                    LOGGER.info("Got JSON array of length {}.", valuesArrayNode.size());
                    
                    Iterator<JsonNode> valuesIterator = valuesArrayNode.iterator();
                    
                    getStartAndEndDateTime(valuesIterator);
                }

        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        fewObject.setTimeStepMultiplier(timeStepMultiplier);
        return this;
    }
    
    public void fillEventList() {
        
        Iterator<JsonNode> valuesIterator = valuesArrayNode.iterator();
        
        DateTime currentHourDT = null;
        
        while (valuesIterator.hasNext()) {
            JsonNode jsonNode = (JsonNode) valuesIterator.next();

            JsonNode timeStampNode = jsonNode.path("timestamp");
            JsonNode valueNode = jsonNode.path("value");
           
            String timeString = TimeUtils.convertFromUNIXTime(timeStampNode.asText());
            
            DateTime dateTime = DateTime.parse(timeString);
            
            DateTime fullHour = dateTime.hourOfDay().roundFloorCopy();
            String value = valueNode.asText();
            
            if(fullHour.isBefore(startDateTime)){
                continue;
            }
            if(fullHour.isAfter(endDateTime)){
                break;
            }
            
            if(currentHourDT != null){
                
                if(fullHour.isAfter(currentHourDT)){
                        
                    fillUpGap(currentHourDT, fullHour, value);
                        
                    currentHourDT = fullHour;                                
                }
                
            }else{                            
                currentHourDT = fullHour;
                
                eventList.add(new Event().setDateTime(fullHour).setValue(value));                            
            }
        }
        
        fewObject.setStartDate(startDate);
        fewObject.setStartTime(startTime);
        fewObject.setEndDate(endDate);
        fewObject.setEndTime(endTime);
        fewObject.setEventList(eventList);
        
    }

    private void getStartAndEndDateTime(Iterator<JsonNode> valuesIterator) {
        
        while (valuesIterator.hasNext()) {
            JsonNode jsonNode = (JsonNode) valuesIterator.next();

            JsonNode timeStampNode = jsonNode.path("timestamp");
           
            String timeString = TimeUtils.convertFromUNIXTime(timeStampNode.asText());
            
            DateTime dateTime = DateTime.parse(timeString);
            
            DateTime fullHour = dateTime.hourOfDay().roundFloorCopy();
            
            if(startDate == null || startDate.isEmpty()){
                startDate = fullHour.toString(TimeUtils.DATE_FORMATTER);
                startTime = fullHour.toString(TimeUtils.TIME_FORMATTER);
            }
            
            if(!valuesIterator.hasNext()){
                endDate = fullHour.toString(TimeUtils.DATE_FORMATTER);
                endTime = fullHour.toString(TimeUtils.TIME_FORMATTER);
            }
        }
        
    }

    public void writeFEWObject(){

        Writer stream = new OutputStreamWriter(outputStream);

        new TimeSeriesToTalsim().setOutputStream(stream).setFEWObject(fewObject).createTimeSeries();

        try {
            outputStream.close();
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
        
        DateTime currentTime = start;
        
        if(timeStepMultiplier.equals("900")){
            
            for (int i = 0; i < hours.getHours(); i++) {
                currentTime = start.plusHours(i);
                for (int j = 0; j <= 3; j++) {
                    int minutes = 15 * (j+1);
                    eventList.add(new Event().setDateTime(currentTime.plusMinutes(minutes)).setValue(value));
//                    eventList.add(new Event().setDateTime(start.plusHours(i)).setValue(value));
                }
            }
            
        }else if(timeStepMultiplier.equals("1800")){
            
            for (int i = 0; i < hours.getHours(); i++) {
                currentTime = start.plusHours(i);
                for (int j = 0; j <= 1; j++) {
                    int minutes = 30 * (j+1);
                    eventList.add(new Event().setDateTime(currentTime.plusMinutes(minutes)).setValue(value));
                }
            }
            
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setStartDateTime(DateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public void setEndDateTime(DateTime endDateTime) {
        this.endDateTime = endDateTime;
    }
}
