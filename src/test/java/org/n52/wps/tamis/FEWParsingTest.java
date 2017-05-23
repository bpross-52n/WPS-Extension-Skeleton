package org.n52.wps.tamis;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.junit.Test;

import nl.wldelft.fews.pi.TimeSeriesComplexType;
import nl.wldelft.fews.pi.TimeSeriesDocument;

public class FEWParsingTest {

    @Test
    public void parseFEWObject(){
        
        try {
            TimeSeriesDocument timeSeriesDocument = TimeSeriesDocument.Factory.parse(getClass().getResourceAsStream("TalsimResult.xml"));

            TimeSeriesComplexType seriesArray = timeSeriesDocument.getTimeSeries().getSeriesArray(0);
            
            System.out.println(new TalsimToTimeseries().createTimeseriesJSONFromTimeseriesXML(seriesArray));
            
//            System.out.println(timeSeriesDocument.getTimeSeries().getSeriesArray().length);
//            
//            TimeSeriesComplexType seriesArray = timeSeriesDocument.getTimeSeries().getSeriesArray(0);
//            
//            seriesArray.getEventArray();
            
        } catch (XmlException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
