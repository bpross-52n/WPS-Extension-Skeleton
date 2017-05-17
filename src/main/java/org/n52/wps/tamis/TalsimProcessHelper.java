package org.n52.wps.tamis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TalsimProcessHelper {

    private static Logger LOGGER = LoggerFactory.getLogger(TalsimProcessHelper.class);

    private final String dischargeFilename = "abgabe.xml";

    private final String volumeFilename = "volumen.xml";

    private final String inflowFilename = "zufluss.xml";

    private String talsimFEWStoTALSIMDataPath;
    
    private InputStream dischargeInputStream;
    
    private InputStream volumeInputStream;
    
    private InputStream inflowInputStream;
    
    public void createTalsimInputs(){
        
        File dischargeInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + dischargeFilename);
        File volumeInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + volumeFilename);
        File inflowInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + inflowFilename);

        // create discharge input for Talsim
        TimeSeriesAPIResponseHandler dischargeTimeSeriesAPIResponseHandler = null;
        FileOutputStream dischargeFileOutputStream = null;
        
        try {

            dischargeFileOutputStream = new FileOutputStream(dischargeInputFile);

            FEWObject dischargeFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                    .setLocationId(FEWObject.LocationID.TBEV.toString())
                    .setParameterId(FEWObject.ParameterId.QA1.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());
            try {
                
                dischargeTimeSeriesAPIResponseHandler = new TimeSeriesAPIResponseHandler().setInputStream(dischargeInputStream)
                        .setOutputStream(dischargeFileOutputStream).setFEWObject(dischargeFEWObject).prepareFEWObject();
                
            } catch (Exception e) {
                LOGGER.error("Could not create file: " + dischargeInputFile, e);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not create Talsim input file for discharge.");
        }
        
        DateTime actualStartInstant = TimeUtils.getDateTime(dischargeTimeSeriesAPIResponseHandler.getStartDate(), dischargeTimeSeriesAPIResponseHandler.getStartTime());
        DateTime actualEndInstant = TimeUtils.getDateTime(dischargeTimeSeriesAPIResponseHandler.getEndDate(), dischargeTimeSeriesAPIResponseHandler.getEndTime());
        
        // create volume input for Talsim
        TimeSeriesAPIResponseHandler volumeTimeSeriesAPIResponseHandler = null;
        FileOutputStream volumeFileOutputStream = null;
        
        try {

            volumeFileOutputStream = new FileOutputStream(volumeInputFile);

            FEWObject volumeFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                    .setLocationId(FEWObject.LocationID.TBEV.toString())
                    .setParameterId(FEWObject.ParameterId.Volumen.getParameterID()).setUnits(FEWObject.Unit.millionm3.getUnitForTalSIM());
            try {
                
                volumeTimeSeriesAPIResponseHandler = new TimeSeriesAPIResponseHandler().setInputStream(volumeInputStream)
                        .setOutputStream(volumeFileOutputStream).setFEWObject(volumeFEWObject).prepareFEWObject();
                
            } catch (Exception e) {
                LOGGER.error("Could not create file: " + volumeInputFile, e);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not create Talsim input file for volume.");
        }

        DateTime volumeStartInstant = TimeUtils.getDateTime(volumeTimeSeriesAPIResponseHandler.getStartDate(), volumeTimeSeriesAPIResponseHandler.getStartTime());
        DateTime volumeEndInstant = TimeUtils.getDateTime(volumeTimeSeriesAPIResponseHandler.getEndDate(), volumeTimeSeriesAPIResponseHandler.getEndTime());

        if(volumeStartInstant.isAfter(actualStartInstant)){
            actualStartInstant = volumeStartInstant;
        }
        if(volumeEndInstant.isBefore(actualEndInstant)){
            actualEndInstant = volumeEndInstant;
        }
        
        // create inflow input for Talsim
        TimeSeriesAPIResponseHandler inflowTimeSeriesAPIResponseHandler = null;
        FileOutputStream inflowFileOutputStream = null;
        
        try {

            inflowFileOutputStream = new FileOutputStream(inflowInputFile);

            FEWObject inflowFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                    .setLocationId(FEWObject.LocationID.EBEV.toString())
                    .setParameterId(FEWObject.ParameterId.Zufluss.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());
            try {
                
                inflowTimeSeriesAPIResponseHandler = new TimeSeriesAPIResponseHandler().setInputStream(inflowInputStream)
                        .setOutputStream(inflowFileOutputStream).setFEWObject(inflowFEWObject).prepareFEWObject();
                
            } catch (Exception e) {
                LOGGER.error("Could not create file: " + inflowInputFile, e);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not create Talsim input file for inflow.");
        }
        
        DateTime inflowStartInstant = TimeUtils.getDateTime(inflowTimeSeriesAPIResponseHandler.getStartDate(), inflowTimeSeriesAPIResponseHandler.getStartTime());
        DateTime inflowEndInstant = TimeUtils.getDateTime(inflowTimeSeriesAPIResponseHandler.getEndDate(), inflowTimeSeriesAPIResponseHandler.getEndTime());

        if(inflowStartInstant.isAfter(actualStartInstant)){
            actualStartInstant = inflowStartInstant;
        }
        if(inflowEndInstant.isBefore(actualEndInstant)){
            actualEndInstant = inflowEndInstant;
        }
        
        String[] actualStartDateTimeStringArray = TimeUtils.getDateAndTime(actualStartInstant);
        
        String actualStartDate = actualStartDateTimeStringArray[0];
        String actualStartTime = actualStartDateTimeStringArray[1];
        
        String[] actualEndDateTimeStringArray = TimeUtils.getDateAndTime(actualEndInstant);
        
        String actualEndDate = actualEndDateTimeStringArray[0];
        String actualEndTime = actualEndDateTimeStringArray[1];
        
        dischargeTimeSeriesAPIResponseHandler.setStartDate(actualStartDate);
        dischargeTimeSeriesAPIResponseHandler.setStartTime(actualStartTime);
        volumeTimeSeriesAPIResponseHandler.setStartDate(actualStartDate);
        volumeTimeSeriesAPIResponseHandler.setStartTime(actualStartTime);
        inflowTimeSeriesAPIResponseHandler.setStartDate(actualStartDate);
        inflowTimeSeriesAPIResponseHandler.setStartTime(actualStartTime);
        
        dischargeTimeSeriesAPIResponseHandler.setEndDate(actualEndDate);
        dischargeTimeSeriesAPIResponseHandler.setEndTime(actualEndTime);
        volumeTimeSeriesAPIResponseHandler.setEndDate(actualEndDate);
        volumeTimeSeriesAPIResponseHandler.setEndTime(actualEndTime);
        inflowTimeSeriesAPIResponseHandler.setEndDate(actualEndDate);
        inflowTimeSeriesAPIResponseHandler.setEndTime(actualEndTime);
        
        dischargeTimeSeriesAPIResponseHandler.fillEventList();
        volumeTimeSeriesAPIResponseHandler.fillEventList();
        inflowTimeSeriesAPIResponseHandler.fillEventList();
        
        dischargeTimeSeriesAPIResponseHandler.writeFEWObject();
        volumeTimeSeriesAPIResponseHandler.writeFEWObject();
        inflowTimeSeriesAPIResponseHandler.writeFEWObject();
        
    }

    public void setTalsimFEWStoTALSIMDataPath(String talsimFEWStoTALSIMDataPath) {
        this.talsimFEWStoTALSIMDataPath = talsimFEWStoTALSIMDataPath;
    }

    public void setDischargeInputStream(InputStream dischargeInputStream) {
        this.dischargeInputStream = dischargeInputStream;
    }

    public void setVolumeInputStream(InputStream volumeInputStream) {
        this.volumeInputStream = volumeInputStream;
    }
    
    public void setInflowInputStream(InputStream inflowInputStream) {
        this.inflowInputStream = inflowInputStream;
    }
    
}
