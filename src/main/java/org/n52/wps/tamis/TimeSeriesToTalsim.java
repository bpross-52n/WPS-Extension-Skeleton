package org.n52.wps.tamis;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javanet.staxutils.IndentingXMLStreamWriter;

public class TimeSeriesToTalsim {

    private static Logger LOGGER = LoggerFactory.getLogger(TimeSeriesToTalsim.class);

    private final String namespace = "http://www.wldelft.nl/fews/PI";

    private final String schemaLocation = "http://fews.wldelft.nl/schemas/version1.0/pi-schemas/pi_timeseries.xsd";

    private final String timeseriesElement = "TimeSeries";

    private final String timeseriesVersion = "1.2";

    private final String versionAttribute = "version";

    private final String timeZoneElement = "timeZone";

    private final String timeZoneValue = "0.0";

    private String typeElement = "type";

    private String locationIdElement = "locationId";

    private String parameterIdElement = "parameterId";

    private String timeStepElement = "timeStep";

    private String timeStepUnitAttribute = "unit";

    private String timeStepMultiplierAttribute = "multiplier";

    private String startDateElement = "startDate";

    private String endDateElement = "endDate";

    private String dateAttribute = "date";

    private String timeAttribute = "time";

    private String missValElement = "missVal";

    private String stationNameElement = "stationName";

    private String unitsElement = "units";
    
    private String headerElement = "header";
    
    private String seriesElement = "series";
    
    private String eventElement = "event";
    
    private String valueAttribute = "value";

    private String startDate;

    private String startTime;

    private String endDate;

    private String endTime;

    private Writer stream;
    
    private List<Event> events;

    private FEWObject fewObject;
    
    public TimeSeriesToTalsim() {}
    
    public void createTimeSeries(){  
        
        checkPrerequisites();

        XMLOutputFactory xof = XMLOutputFactory.newInstance();

        XMLStreamWriter xtw;
        try {
            xtw = xof.createXMLStreamWriter(stream);
        } catch (XMLStreamException e) {
            LOGGER.error("");
            LOGGER.error(e.getMessage());
            return;
        }
        
        XMLStreamWriter idxtw = new IndentingXMLStreamWriter(xtw);

        try {
            createDocumentStart(idxtw);
        } catch (XMLStreamException e) {
            LOGGER.error("Could not create TimeSeries XML document start.");
            LOGGER.error(e.getMessage());
        }

        try {
            idxtw.writeStartElement(seriesElement);
        } catch (XMLStreamException e) {
            LOGGER.error("Could write series start element.");
            LOGGER.error(e.getMessage());
        }
        
        try {
            createHeader(idxtw);
        } catch (XMLStreamException e) {
            LOGGER.error("Could not create TimeSeries XML document header.");
            LOGGER.error(e.getMessage());
        }
        
        try {
            createEvents(idxtw);
        } catch (XMLStreamException e) {
            LOGGER.error("Could not create TimeSeries XML document events.");
            LOGGER.error(e.getMessage());
        }

        try {
            createDocumentEnd(idxtw);
        } catch (XMLStreamException e) {
            LOGGER.error("Could not create TimeSeries XML document end.");
            LOGGER.error(e.getMessage());
        }

        try {
            idxtw.flush();
        } catch (XMLStreamException e) {
            LOGGER.error("Could not flush XMLStreamWriter.");
            LOGGER.error(e.getMessage());
        }
    }

    private void checkPrerequisites() {

        //check stream
        if(stream == null){
            throw new IllegalArgumentException("Writer not set.");
        }
        
        //check parameters
        Map<String, String> parameterMap = new HashMap<>();

        parameterMap.put("type", getType());
        parameterMap.put("locationId", getLocationId());
        parameterMap.put("parameterId", getParameterId());
        parameterMap.put("timeStepUnit", getTimeStepUnit());
        parameterMap.put("timeStepMultiplier", getTimeStepMultiplier());
        parameterMap.put("startDate", getStartDate());
        parameterMap.put("startTime", getStartTime());
        parameterMap.put("endDate", getEndDate());
        parameterMap.put("endTime", getEndTime());
        parameterMap.put("missVal", getMissVal());
        parameterMap.put("stationName", getStationName());
        parameterMap.put("units", getUnits());

        for (String parameterName : parameterMap.keySet()) {
            
            String parameterValue = parameterMap.get(parameterName);
            
            if (parameterValue == null || parameterValue.equals("")) {
                throw new IllegalArgumentException(String.format("Parameter %s not present.", parameterName));
            }            
        }
        
        //TODO check timeseries, best including check form matching start/end date/time
        if(events == null || events.isEmpty()){
            throw new IllegalArgumentException("No events (date/time/value) for writing.");
        }
    }

    private void createEvents(XMLStreamWriter idxtw) throws XMLStreamException {        
        for (Event event : events) {
            idxtw.writeEmptyElement(eventElement);
            idxtw.writeAttribute(dateAttribute, event.getDate());
            idxtw.writeAttribute(timeAttribute, event.getTime());
            idxtw.writeAttribute(valueAttribute, event.getValue());
//            idxtw.writeAttribute(flagAttribute, event.getFlag());
        }        
    }
    
    
    
    private void createDocumentStart(XMLStreamWriter idxtw) throws XMLStreamException {
        idxtw.setDefaultNamespace(namespace);
        idxtw.writeStartDocument("utf-8", "1.0");
        idxtw.writeStartElement(namespace, timeseriesElement);
        idxtw.writeNamespace("xsi", "http://www.w3.org/2000/10/XMLSchema-instance");
        idxtw.writeAttribute("http://www.w3.org/2000/10/XMLSchema-instance", "schemaLocation", namespace + " " + schemaLocation);
        idxtw.writeAttribute(versionAttribute, timeseriesVersion);
        idxtw.writeDefaultNamespace(namespace);
        idxtw.writeStartElement(namespace, timeZoneElement);
        idxtw.writeCharacters(timeZoneValue);
        idxtw.writeEndElement();
    }

    private void createDocumentEnd(XMLStreamWriter idxtw) throws XMLStreamException {
        idxtw.writeEndElement();// series
        idxtw.writeEndElement();// TimeSeries
        idxtw.writeEndDocument();
    }

    private void createHeader(XMLStreamWriter idxtw) throws XMLStreamException {

        idxtw.writeStartElement(namespace, headerElement);
        
        // type
        idxtw.writeStartElement(namespace, typeElement);
        idxtw.writeCharacters(getType());
        idxtw.writeEndElement();

        // locationId
        idxtw.writeStartElement(namespace, locationIdElement);
        idxtw.writeCharacters(getLocationId());
        idxtw.writeEndElement();

        // parameterId
        idxtw.writeStartElement(namespace, parameterIdElement);
        idxtw.writeCharacters(getParameterId());
        idxtw.writeEndElement();

        // timeStep
        idxtw.writeStartElement(namespace, timeStepElement);
        idxtw.writeAttribute(timeStepUnitAttribute, getTimeStepUnit());
        idxtw.writeAttribute(timeStepMultiplierAttribute, getTimeStepMultiplier());
        idxtw.writeEndElement();

        // startDate
        idxtw.writeStartElement(namespace, startDateElement);
        idxtw.writeAttribute(timeAttribute, getStartTime());
        idxtw.writeAttribute(dateAttribute, getStartDate());
        idxtw.writeEndElement();

        // endDate
        idxtw.writeStartElement(namespace, endDateElement);
        idxtw.writeAttribute(timeAttribute, getEndTime());
        idxtw.writeAttribute(dateAttribute, getEndDate());
        idxtw.writeEndElement();

        // missVal
        idxtw.writeStartElement(namespace, missValElement);
        idxtw.writeCharacters(getMissVal());
        idxtw.writeEndElement();

        // stationName
        idxtw.writeStartElement(namespace, stationNameElement);
        idxtw.writeCharacters(getStationName());
        idxtw.writeEndElement();

        // units
        idxtw.writeStartElement(namespace, unitsElement);
        idxtw.writeCharacters(getUnits());
        idxtw.writeEndElement();
        
        idxtw.writeEndElement();//header
    }

    public String getType() {
        return fewObject.getType();
    }

    public String getLocationId() {
        return fewObject.getLocationId();
    }

    public String getParameterId() {
        return fewObject.getParameterId();
    }

    public String getTimeStepUnit() {
        return fewObject.getTimeStepUnit();
    }

    public String getTimeStepMultiplier() {
        return fewObject.getTimeStepMultiplier();
    }

    public String getStartDate() {
        return startDate;
    }

    public TimeSeriesToTalsim setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate() {
        return endDate;
    }

    public TimeSeriesToTalsim setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getStartTime() {
        return startTime;
    }

    public TimeSeriesToTalsim setStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    public String getEndTime() {
        return endTime;
    }

    public TimeSeriesToTalsim setEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    public String getMissVal() {
        return fewObject.getMissVal();
    }

    public String getStationName() {
        return fewObject.getStationName();
    }

    public String getUnits() {
        return fewObject.getUnits();
    }

    public Writer getStream() {
        return stream;
    }

    public TimeSeriesToTalsim setOutputStream(Writer stream) {
        this.stream = stream;
        return this;
    }

    public List<Event> getEvents() {
        return events;
    }

    public TimeSeriesToTalsim setEvents(List<Event> events) {
        this.events = events;
        return this;
    }

    public FEWObject getFEWObject() {
        return fewObject;
    }

    public TimeSeriesToTalsim setFEWObject(FEWObject fewObject) {
        this.fewObject = fewObject;
        return this;
    }
    
}
