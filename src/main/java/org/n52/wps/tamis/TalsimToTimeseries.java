package org.n52.wps.tamis;

import org.n52.iceland.ogc.gml.AbstractFeature;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;

public class TalsimToTimeseries {

    public TalsimToTimeseries(FEWObject fewObject){
        
        OmObservation result = createObservationfromFEWObject(fewObject);
        
    }
    
    public OmObservation createObservationfromFEWObject(FEWObject fewObject){
        
        OmObservation result = new OmObservation();
        
//        result.setIdentifier("o_1");
        
        result.setGmlId("o_1");
        
        OmObservationConstellation observationConstellation = new OmObservationConstellation();
        
        AbstractFeature featureOfInterest = null;
        
        observationConstellation.setFeatureOfInterest(featureOfInterest);
        
        result.setObservationConstellation(observationConstellation);
        
        return result;
        
    }
    
}
