package org.n52.wps.tamis;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import org.n52.wps.webapp.common.AbstractITClass;

public class TimeSeriesToTalsimTest extends AbstractITClass {

    @Test
    public void writeHeader() throws IOException {

        FEWObject fewObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                .setLocationId(FEWObject.LocationID.TBEV.toString())
                .setParameterId(FEWObject.ParameterId.QA1.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());

//        URL url = new URL(
//                "http://fluggs.wupperverband.de/sos2-tamis/service?service=SOS&version=2.0.0&request=GetObservation&offering=Zeitreihen_Einzelwert&observedProperty=Speicherinhalt&procedure=Einzelwert&namespaces=xmlns(sams%2Chttp%3A%2F%2Fwww.opengis.net%2FsamplingSpatial%2F2.0)%2Cxmlns(om%2Chttp%3A%2F%2Fwww.opengis.net%2Fom%2F2.0)&featureOfInterest=Bever-Talsperre_Windenhaus&temporalFilter=om%3AphenomenonTime%2C2016-06-01T00%3A00%3A00.000Z%2F2016-06-10T23%3A59%3A59.999Z&responseFormat=http://www.opengis.net/om/2.0");
//        URL url = new URL(
//                "http://fluggs.wupperverband.de/sos2-tamis/service?service=SOS&version=2.0.0&request=GetObservation&offering=Zeitreihen_Einzelwert&observedProperty=Zufluss&procedure=Einzelwert&namespaces=xmlns(sams%2Chttp%3A%2F%2Fwww.opengis.net%2FsamplingSpatial%2F2.0)%2Cxmlns(om%2Chttp%3A%2F%2Fwww.opengis.net%2Fom%2F2.0)&featureOfInterest=Bever-Talsperre_Mitte&temporalFilter=om%3AphenomenonTime%2C2016-06-01T00%3A00%3A00.000Z%2F2016-06-10T23%3A59%3A59.999Z&responseFormat=http://www.opengis.net/om/2.0");
//        URL url = new URL(
//                "http://fluggs.wupperverband.de/sos2-tamis/service?service=SOS&version=2.0.0&request=GetObservation&offering=Zeitreihen_Einzelwert&observedProperty=Abfluss&procedure=Einzelwert&namespaces=xmlns(sams%2Chttp%3A%2F%2Fwww.opengis.net%2FsamplingSpatial%2F2.0)%2Cxmlns(om%2Chttp%3A%2F%2Fwww.opengis.net%2Fom%2F2.0)&featureOfInterest=Reinshagensbever&temporalFilter=om%3AphenomenonTime%2C2016-06-01T00%3A00%3A00.000Z%2F2016-06-10T23%3A59%3A59.999Z&responseFormat=http://www.opengis.net/om/2.0");
//
//        new GetObservationResponseHandler().setOutputStream(System.out).setInputStream(Util.connectWithBasicAuth(url.toString(), "tamis", "vmV#GnX?U837.8,?"))
//                .setFEWObject(fewObject).handle();
        
        FileOutputStream fileOutputStream = new FileOutputStream("d:/tmp/talsim" + UUID.randomUUID());
        
        TimeSeriesAPIResponseHandler apiResponseHandler = new TimeSeriesAPIResponseHandler().setOutputStream(fileOutputStream).setInputStream(getClass().getResourceAsStream("example.json"))
        .setFEWObject(fewObject).prepareFEWObject();
        
        apiResponseHandler.fillEventList();
        
        apiResponseHandler.writeFEWObject();
    }

}
