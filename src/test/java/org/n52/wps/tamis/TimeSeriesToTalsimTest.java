package org.n52.wps.tamis;

import org.junit.Test;
import org.n52.wps.webapp.common.AbstractITClass;

public class TimeSeriesToTalsimTest extends AbstractITClass {

    private String type = "instantaneous";

    private String locationId = "TBEV";

    private String parameterId = "QA1";

    private String timeStepUnit = "second";
    
    private String timeStepMultiplier = "900";

    private String startDate = "2014-02-10";

    private String startTime = "00:00:00";

    private String endDate = "2014-02-20";

    private String endTime = "00:00:00";

    private String missVal = "-999.0";

    private String stationName = "Bevertalsperre";

    private String units = "m3/s";

    @Test
    public void writeHeader() {
        
        new GetObservationResponseHandler().setStream(getClass().getResourceAsStream("zufluss_om.xml")).setLocationId(locationId).setParameterId(parameterId).setMissVal(missVal).setTimeStepMultiplier(timeStepMultiplier).setTimeStepUnit(timeStepUnit).setType(type).handle();
        
//        Writer stream = new OutputStreamWriter(System.out);
//        
//        new TimeSeriesToTalsim().setStream(stream).setType(type).setLocationId(locationId).setParameterId(parameterId).setTimeStepUnit(timeStepUnit).setTimeStepMultiplier(timeStepMultiplier).setStartDate(startDate)
//                .setStartTime(startTime).setEndDate(endDate).setEndTime(endTime).setMissVal(missVal).setStationName(stationName).setUnits(units).createTimeSeries();

    }

}
