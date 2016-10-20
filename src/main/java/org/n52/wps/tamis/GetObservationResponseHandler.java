package org.n52.wps.tamis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.om.x20.OMObservationType;
import net.opengis.sos.x20.GetObservationResponseDocument;
import net.opengis.sos.x20.GetObservationResponseType.ObservationData;

public class GetObservationResponseHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(GetObservationResponseHandler.class);

    private InputStream inputStream;

    private String startDate;

    private String startTime;

    private String endDate;

    private String endTime;

    private String missVal;

    private String units;

    private FEWObject fewObject;

    private List<Event> eventList;

	private OutputStream outputStream;

    public GetObservationResponseHandler() {
        eventList = new ArrayList<>();
    }

    public void handle() {

        checkPrerequisites();

        try {
            GetObservationResponseDocument response = GetObservationResponseDocument.Factory.parse(inputStream);

            ObservationData[] dataArray = response.getGetObservationResponse().getObservationDataArray();

            LOGGER.info("Got {} observations.", dataArray.length);

            OMObservationType observation = dataArray[0].getOMObservation();//TODO check

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
        
        fewObject.setUnits(units);
        fewObject.setEventList(eventList);
        fewObject.setStartDate(startDate);
        fewObject.setStartTime(startTime);
        fewObject.setEndDate(endDate);
        fewObject.setEndTime(endTime);
        
        Writer stream = new OutputStreamWriter(outputStream);
        
        new TimeSeriesToTalsim().setOutputStream(stream).setFEWObject(fewObject).createTimeSeries();

        try {
			stream.close();
		} catch (IOException e) {
			/* ignore */
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

    public GetObservationResponseHandler setInputStream(InputStream stream) {
        this.inputStream = stream;
        return this;
    }
    
    public GetObservationResponseHandler setOutputStream(OutputStream stream) {
    	this.outputStream = stream;
    	return this;
    }

    public FEWObject getFEWObject() {
        return fewObject;
    }

    public GetObservationResponseHandler setFEWObject(FEWObject fewObject) {
        this.fewObject = fewObject;
        return this;
    }
}
