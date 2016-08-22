package org.n52.wps.tamis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.n52.iceland.exception.ows.OwsExceptionReport;
import org.n52.iceland.ogc.gml.time.Time;
import org.n52.iceland.ogc.gml.time.TimePeriod;
import org.n52.sos.decode.OmDecoderv20;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.swe.DataRecord;
import org.n52.sos.ogc.swe.SweAbstractDataComponent;
import org.n52.sos.ogc.swe.SweDataArray;
import org.n52.sos.ogc.swe.SweField;
import org.n52.sos.ogc.swe.simpleType.SweQuantity;
import org.n52.sos.util.SosConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.om.x20.OMObservationType;
import net.opengis.sos.x20.GetObservationResponseDocument;
import net.opengis.sos.x20.GetObservationResponseType.ObservationData;

public class GetObservationResponseHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(GetObservationResponseHandler.class);

    private InputStream stream;

    private String type;

    private String locationId;

    private String parameterId;

    private String timeStepUnit;

    private String timeStepMultiplier;

    private String startDate;

    private String startTime;

    private String endDate;

    private String endTime;

    private String missVal;

    private String stationName = "Bevertalsperre";

    private String units;

    private List<Event> eventList;

    public GetObservationResponseHandler() {
        eventList = new ArrayList<>();
    }

    public void handle() {

        checkPrerequisites();

        try {
            GetObservationResponseDocument response = GetObservationResponseDocument.Factory.parse(stream);

            ObservationData[] dataArray = response.getGetObservationResponse().getObservationDataArray();

            LOGGER.info("Got {} observations.", dataArray.length);

            OMObservationType observation = dataArray[0].getOMObservation();//TODO check

            SosConfiguration.init();

            Object parsedObject = new OmDecoderv20().decode(observation);

            if (parsedObject instanceof OmObservation) {
                
                OmObservation omObservation = (OmObservation)parsedObject;

                Time phenomenonTime = omObservation.getPhenomenonTime();
                
                if(phenomenonTime instanceof TimePeriod){
                    
                    TimePeriod timePeriod = (TimePeriod)phenomenonTime;
                    
                    DateTime start = timePeriod.getStart();
                    DateTime end = timePeriod.getEnd();
                    
                    startDate = start.toString(TimeUtils.DATE_FORMATTER);
                    startTime = start.toString(TimeUtils.TIME_FORMATTER);
                    endDate = end.toString(TimeUtils.DATE_FORMATTER);
                    endTime = end.toString(TimeUtils.TIME_FORMATTER);                    
                }
                
                //get unit of measurement
                if(missVal == null || missVal.equals("")){
                    missVal = omObservation.getNoDataValue() != null ? omObservation.getNoDataValue() : "";
                }
                
                String observedProperty = "";
                
                try {                    
                    observedProperty = omObservation.getObservationConstellation().getObservableProperty().getIdentifier();                    
                } catch (Exception e) {
                    LOGGER.info("Could not fetch observedProperty.");
                }
                
                
                Object value = omObservation.getValue().getValue().getValue();

                if (value instanceof SweDataArray) {
                    SweDataArray sweDataArray = (SweDataArray) value;

                    Object elementType = sweDataArray.getElementType();
                    
                    if(elementType instanceof DataRecord){
                        DataRecord dataRecord = (DataRecord)elementType;
                        for (SweField sweField : dataRecord.getFields()) {
                            if(observedProperty != null && sweField.getName().getValue().equals(observedProperty)){
                                SweAbstractDataComponent element = sweField.getElement();                                
                                if(element instanceof SweQuantity){
                                    units = ((SweQuantity)element).getUom();
                                }                                
                            }
                        }
                    }
                    
                    LOGGER.info("Got SWE data array of leghth {}.", sweDataArray.getElementCount().getValue());

                    for (List<String> sweDataArrayValues : sweDataArray.getValues()) {
                        if (sweDataArrayValues.size() > 1) {
                            DateTime dateTime = DateTime.parse(sweDataArrayValues.get(0));
                            String value1 = sweDataArrayValues.get(1);
                            eventList.add(new Event().setDateTime(dateTime).setValue(value1));
                        }

                    }
                }
            }

        } catch (XmlException | IOException | OwsExceptionReport e) {
            LOGGER.error(e.getMessage());
        }

        Writer stream = new OutputStreamWriter(System.out);

        new TimeSeriesToTalsim().setEvents(eventList).setStream(stream).setType(type).setLocationId(locationId).setParameterId(parameterId).setTimeStepUnit(timeStepUnit)
                .setTimeStepMultiplier(timeStepMultiplier).setStartDate(startDate).setStartTime(startTime).setEndDate(endDate).setEndTime(endTime).setMissVal(missVal).setStationName(stationName)
                .setUnits(units).createTimeSeries();

    }

    private void checkPrerequisites() {
        if (stream == null) {
            throw new IllegalArgumentException("InputStream not set.");
        }
    }

    public GetObservationResponseHandler setStream(InputStream stream) {
        this.stream = stream;
        return this;
    }

    public String getType() {
        return type;
    }

    public GetObservationResponseHandler setType(String type) {
        this.type = type;
        return this;
    }

    public String getLocationId() {
        return locationId;
    }

    public GetObservationResponseHandler setLocationId(String locationId) {
        this.locationId = locationId;
        return this;
    }

    public String getParameterId() {
        return parameterId;
    }

    public GetObservationResponseHandler setParameterId(String parameterId) {
        this.parameterId = parameterId;
        return this;
    }

    public String getTimeStepUnit() {
        return timeStepUnit;
    }

    public GetObservationResponseHandler setTimeStepUnit(String timeStepUnit) {
        this.timeStepUnit = timeStepUnit;
        return this;
    }

    public String getTimeStepMultiplier() {
        return timeStepMultiplier;
    }

    public GetObservationResponseHandler setTimeStepMultiplier(String timeStepMultiplier) {
        this.timeStepMultiplier = timeStepMultiplier;
        return this;
    }

    public String getMissVal() {
        return missVal;
    }

    public GetObservationResponseHandler setMissVal(String missVal) {
        this.missVal = missVal;
        return this;
    }

    public String getStationName() {
        return stationName;
    }

    public GetObservationResponseHandler setStationName(String stationName) {
        this.stationName = stationName;
        return this;
    }

}
