package org.n52.wps.tamis;

import org.junit.Test;
import org.n52.wps.webapp.common.AbstractITClass;

public class TimeSeriesToTalsimTest extends AbstractITClass {

    @Test
    public void writeHeader() {

    	FEWObject fewObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
    			.setLocationId(FEWObject.LocationID.TBEV.toString())
    			.setParameterId(FEWObject.ParameterId.QA1.getParameterID())
    			.setTimeStepMultiplier("900");//TODO calculate from timeseries
    	
		new GetObservationResponseHandler().setOutputStream(System.out).setInputStream(getClass().getResourceAsStream("zufluss_om.xml"))
		.setFEWObject(fewObject).handle();

    }

}
