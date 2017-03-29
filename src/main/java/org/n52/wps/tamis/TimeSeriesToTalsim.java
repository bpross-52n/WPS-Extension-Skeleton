package org.n52.wps.tamis;

import java.io.Writer;
import java.util.HashMap;
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

    private Writer stream;
    
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

        parameterMap.put("type", fewObject.getType());
        parameterMap.put("locationId", fewObject.getLocationId());
        parameterMap.put("parameterId", fewObject.getParameterId());
        parameterMap.put("timeStepUnit", fewObject.getTimeStepUnit());
        parameterMap.put("timeStepMultiplier", fewObject.getTimeStepMultiplier());
        parameterMap.put("startDate", fewObject.getStartDate());
        parameterMap.put("startTime", fewObject.getStartTime());
        parameterMap.put("endDate", fewObject.getEndDate());
        parameterMap.put("endTime", fewObject.getEndTime());
        parameterMap.put("missVal", fewObject.getMissVal());
        parameterMap.put("stationName", fewObject.getStationName());
        parameterMap.put("units", fewObject.getUnits());

        for (String parameterName : parameterMap.keySet()) {
            
            String parameterValue = parameterMap.get(parameterName);
            
            if (parameterValue == null || parameterValue.equals("")) {
                throw new IllegalArgumentException(String.format("Parameter %s not present.", parameterName));
            }            
        }
        
        //TODO check timeseries, best including check form matching start/end date/time
        if(fewObject.getEventList() == null || fewObject.getEventList().isEmpty()){
            throw new IllegalArgumentException("No events (date/time/value) for writing.");
        }
    }

    private void createEvents(XMLStreamWriter idxtw) throws XMLStreamException {        
        for (Event event : fewObject.getEventList()) {
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
        idxtw.writeCharacters(fewObject.getType());
        idxtw.writeEndElement();

        // locationId
        idxtw.writeStartElement(namespace, locationIdElement);
        idxtw.writeCharacters(fewObject.getLocationId());
        idxtw.writeEndElement();

        // parameterId
        idxtw.writeStartElement(namespace, parameterIdElement);
        idxtw.writeCharacters(fewObject.getParameterId());
        idxtw.writeEndElement();

        // timeStep
        idxtw.writeStartElement(namespace, timeStepElement);
        idxtw.writeAttribute(timeStepUnitAttribute,fewObject.getTimeStepUnit());
        idxtw.writeAttribute(timeStepMultiplierAttribute, fewObject.getTimeStepMultiplier());
        idxtw.writeEndElement();

        // startDate
        idxtw.writeStartElement(namespace, startDateElement);
        idxtw.writeAttribute(timeAttribute, fewObject.getStartTime());
        idxtw.writeAttribute(dateAttribute, fewObject.getStartDate());
        idxtw.writeEndElement();

        // endDate
        idxtw.writeStartElement(namespace, endDateElement);
        idxtw.writeAttribute(timeAttribute, fewObject.getEndTime());
        idxtw.writeAttribute(dateAttribute, fewObject.getEndDate());
        idxtw.writeEndElement();

        // missVal
        idxtw.writeStartElement(namespace, missValElement);
        idxtw.writeCharacters(fewObject.getMissVal());
        idxtw.writeEndElement();

        // stationName
        idxtw.writeStartElement(namespace, stationNameElement);
        idxtw.writeCharacters(fewObject.getStationName());
        idxtw.writeEndElement();

        // units
        idxtw.writeStartElement(namespace, unitsElement);
        idxtw.writeCharacters(fewObject.getUnits());
        idxtw.writeEndElement();
        
        idxtw.writeEndElement();//header
    }

    public TimeSeriesToTalsim setOutputStream(Writer stream) {
        this.stream = stream;
        return this;
    }
    
    public TimeSeriesToTalsim setFEWObject(FEWObject fewObject) {
        this.fewObject = fewObject;
        return this;
    }
    
}
