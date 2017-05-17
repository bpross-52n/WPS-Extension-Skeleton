package org.n52.wps.tamis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Properties;

import org.joda.time.DateTime;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;
import org.n52.wps.tamis.module.TaMISProcessConfigModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import n52.talsim_sos_converter.TalsimSosConverter;

@Algorithm(
        version = "0.01")
public class TalsimProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(TalsimProcess.class);

    private final String lineSeparator = System.getProperty("line.separator");

    private final String dischargeFilename = "abgabe.xml";

    private final String volumeFilename = "volumen.xml";

    private final String inflowFilename = "zufluss.xml";

    private final String outputFilename = "TalsimResult.xml";

    private String volumeInput;

    private String dischargeInput;

    private String inflowInput;

    private String inflowOutput;

    private String volumeOutput;

    private String dischargeOutput;

    private String waterlevelOutput;

    private String spillwayDischargeOutput;

    private String userName;

    private String password;

    @LiteralDataOutput(
            identifier = "spillway-discharge-output")
    public String getSpillwayDischargeOutput() {
        return spillwayDischargeOutput;
    }

    @LiteralDataOutput(
            identifier = "waterlevel-output")
    public String getWaterLevelOutput() {
        return waterlevelOutput;
    }

    @LiteralDataOutput(
            identifier = "discharge-output")
    public String getDischargeOutput() {
        return dischargeOutput;
    }

    @LiteralDataOutput(
            identifier = "inflow-output")
    public String getInflowOutput() {
        return inflowOutput;
    }

    @LiteralDataOutput(
            identifier = "volume-output")
    public String getVolumeOutput() {
        return volumeOutput;
    }

    @LiteralDataInput(
            binding = LiteralStringBinding.class, identifier = "discharge-input", minOccurs = 1, maxOccurs = 1)
    public void setDischarge(String discharge) {
        this.dischargeInput = discharge;
    }

    @LiteralDataInput(
            binding = LiteralStringBinding.class, identifier = "volume-input", minOccurs = 1, maxOccurs = 1)
    public void setVolume(String volume) {
        this.volumeInput = volume;
    }

    @LiteralDataInput(
            binding = LiteralStringBinding.class, identifier = "inflow-input", minOccurs = 1, maxOccurs = 1)
    public void setInflow(String inflow) {
        this.inflowInput = inflow;
    }

    @Execute
    public void run() {

        ConfigurationModule configModule = WPSConfig.getInstance().getConfigurationModuleForClass(
                DummyAlgorithmRepository.class.getName(), ConfigurationCategory.REPOSITORY);

        if (!(configModule instanceof TaMISProcessConfigModule)) {
            throw new RuntimeException("TaMISProcessConfigModule not found.");
        }

        TaMISProcessConfigModule taMISProcessConfigModule = (TaMISProcessConfigModule) configModule;

        String talsimTaskManagerPath = taMISProcessConfigModule.getTalsimTaskManagerPath();

        String talsimTALSIMtoFEWSDataPath = taMISProcessConfigModule.getTalsimTALSIMtoFEWSDataPath();

        String talsimFEWStoTALSIMDataPath = taMISProcessConfigModule.getTalsimFEWStoTALSIMDataPath();

        String sosURLString = taMISProcessConfigModule.getSosURL();

        String credentialsPath = taMISProcessConfigModule.getCredentialsPath();
        
        Properties credentialsProperties = new Properties();
        
        try {
            credentialsProperties.load(new FileInputStream(new File(credentialsPath)));
        } catch (IOException e3) {
            LOGGER.error("Could not load credentials for secured Timeseries API: " + credentialsPath, e3);
        }
        
        userName = credentialsProperties.getProperty("username");
        password = credentialsProperties.getProperty("password");
        
        URL sosURL = null;
        try {
            sosURL = new URL(sosURLString);
        } catch (MalformedURLException e2) {
            LOGGER.error("Could not create SOS URL from: " + sosURLString);
        }

        // empty data directories
        boolean clearedInputDirectory = clearDirectory(talsimTALSIMtoFEWSDataPath);
        boolean clearedOutputDirectory = clearDirectory(talsimFEWStoTALSIMDataPath);

        LOGGER.info("Cleared input directory " + clearedInputDirectory + "\nCleared output directory "
                + clearedOutputDirectory);

        InputStream dischargeInputStream = null;
        InputStream volumeInputStream = null;
        InputStream inflowInputStream = null;
        
        try {
            dischargeInputStream = Util.connectWithBasicAuth(dischargeInput, userName, password);
            volumeInputStream = Util.connectWithBasicAuth(volumeInput, userName, password);
            inflowInputStream = Util.connectWithBasicAuth(inflowInput, userName, password);
        } catch (IOException e2) {
            LOGGER.error("Could not connect to secured Timeseries API.", e2);
        }
        
        TalsimProcessHelper talsimProcessHelper = new TalsimProcessHelper();
        
        talsimProcessHelper.setDischargeInputStream(dischargeInputStream);
        talsimProcessHelper.setInflowInputStream(inflowInputStream);
        talsimProcessHelper.setTalsimFEWStoTALSIMDataPath(talsimFEWStoTALSIMDataPath);
        talsimProcessHelper.setVolumeInputStream(volumeInputStream);
        
        talsimProcessHelper.createTalsimInputs();
        File outputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + outputFilename);
        
//        File dischargeInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + dischargeFilename);
//        File volumeInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + volumeFilename);
//        File inflowInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + inflowFilename);
//
//        // create discharge input for Talsim
//        TimeSeriesAPIResponseHandler dischargeTimeSeriesAPIResponseHandler = null;
//        FileOutputStream dischargeFileOutputStream = null;
//        
//        try {
//
//            dischargeFileOutputStream = new FileOutputStream(dischargeInputFile);
//
//            FEWObject dischargeFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
//                    .setLocationId(FEWObject.LocationID.TBEV.toString())
//                    .setParameterId(FEWObject.ParameterId.QA1.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());
//            try {
//                
//                dischargeTimeSeriesAPIResponseHandler = new TimeSeriesAPIResponseHandler().setInputStream(Util.connectWithBasicAuth(dischargeInput, userName, password))
//                        .setOutputStream(dischargeFileOutputStream).setFEWObject(dischargeFEWObject).prepareFEWObject();
//                
//            } catch (Exception e) {
//                LOGGER.error("Could not fetch timeseries from: " + dischargeInput, e);
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("Could not create Talsim input file for discharge.");
//        }
//        
//        DateTime actualStartInstant = TimeUtils.getDateTime(dischargeTimeSeriesAPIResponseHandler.getStartDate(), dischargeTimeSeriesAPIResponseHandler.getStartTime());
//        DateTime actualEndInstant = TimeUtils.getDateTime(dischargeTimeSeriesAPIResponseHandler.getEndDate(), dischargeTimeSeriesAPIResponseHandler.getEndTime());
//        
//        // create volume input for Talsim
//        TimeSeriesAPIResponseHandler volumeTimeSeriesAPIResponseHandler = null;
//        FileOutputStream volumeFileOutputStream = null;
//        
//        try {
//
//            volumeFileOutputStream = new FileOutputStream(volumeInputFile);
//
//            FEWObject volumeFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
//                    .setLocationId(FEWObject.LocationID.TBEV.toString())
//                    .setParameterId(FEWObject.ParameterId.Volumen.getParameterID()).setUnits(FEWObject.Unit.millionm3.getUnitForTalSIM());
//            try {
//                
//                volumeTimeSeriesAPIResponseHandler = new TimeSeriesAPIResponseHandler().setInputStream(Util.connectWithBasicAuth(volumeInput, userName, password))
//                        .setOutputStream(volumeFileOutputStream).setFEWObject(volumeFEWObject).prepareFEWObject();
//                
//            } catch (Exception e) {
//                LOGGER.error("Could not fetch timeseries from: " + volumeInput, e);
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("Could not create Talsim input file for volume.");
//        }
//
//        DateTime volumeStartInstant = TimeUtils.getDateTime(volumeTimeSeriesAPIResponseHandler.getStartDate(), volumeTimeSeriesAPIResponseHandler.getStartTime());
//        DateTime volumeEndInstant = TimeUtils.getDateTime(volumeTimeSeriesAPIResponseHandler.getEndDate(), volumeTimeSeriesAPIResponseHandler.getEndTime());
//
//        if(volumeStartInstant.isAfter(actualStartInstant)){
//            actualStartInstant = volumeStartInstant;
//        }
//        if(volumeEndInstant.isBefore(actualEndInstant)){
//            actualEndInstant = volumeEndInstant;
//        }
//        
//        // create inflow input for Talsim
//        TimeSeriesAPIResponseHandler inflowTimeSeriesAPIResponseHandler = null;
//        FileOutputStream inflowFileOutputStream = null;
//        
//        try {
//
//            inflowFileOutputStream = new FileOutputStream(inflowInputFile);
//
//            FEWObject inflowFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
//                    .setLocationId(FEWObject.LocationID.EBEV.toString())
//                    .setParameterId(FEWObject.ParameterId.Zufluss.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());
//            try {
//                
//                inflowTimeSeriesAPIResponseHandler = new TimeSeriesAPIResponseHandler().setInputStream(Util.connectWithBasicAuth(inflowInput, userName, password))
//                        .setOutputStream(inflowFileOutputStream).setFEWObject(inflowFEWObject).prepareFEWObject();
//                
//            } catch (Exception e) {
//                LOGGER.error("Could not fetch timeseries from: " + inflowInput, e);
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("Could not create Talsim input file for inflow.");
//        }
//        
//        DateTime inflowStartInstant = TimeUtils.getDateTime(inflowTimeSeriesAPIResponseHandler.getStartDate(), inflowTimeSeriesAPIResponseHandler.getStartTime());
//        DateTime inflowEndInstant = TimeUtils.getDateTime(inflowTimeSeriesAPIResponseHandler.getEndDate(), inflowTimeSeriesAPIResponseHandler.getEndTime());
//
//        if(inflowStartInstant.isAfter(actualStartInstant)){
//            actualStartInstant = inflowStartInstant;
//        }
//        if(inflowEndInstant.isBefore(actualEndInstant)){
//            actualEndInstant = inflowEndInstant;
//        }
//        
//        String[] actualStartDateTimeStringArray = TimeUtils.getDateAndTime(actualStartInstant);
//        
//        String actualStartDate = actualStartDateTimeStringArray[0];
//        String actualStartTime = actualStartDateTimeStringArray[1];
//        
//        String[] actualEndDateTimeStringArray = TimeUtils.getDateAndTime(actualEndInstant);
//        
//        String actualEndDate = actualEndDateTimeStringArray[0];
//        String actualEndTime = actualEndDateTimeStringArray[1];
//        
//        dischargeTimeSeriesAPIResponseHandler.setStartDate(actualStartDate);
//        dischargeTimeSeriesAPIResponseHandler.setStartTime(actualStartTime);
//        volumeTimeSeriesAPIResponseHandler.setStartDate(actualStartDate);
//        volumeTimeSeriesAPIResponseHandler.setStartTime(actualStartTime);
//        inflowTimeSeriesAPIResponseHandler.setStartDate(actualStartDate);
//        inflowTimeSeriesAPIResponseHandler.setStartTime(actualStartTime);
//        
//        dischargeTimeSeriesAPIResponseHandler.setEndDate(actualEndDate);
//        dischargeTimeSeriesAPIResponseHandler.setEndTime(actualEndTime);
//        volumeTimeSeriesAPIResponseHandler.setEndDate(actualEndDate);
//        volumeTimeSeriesAPIResponseHandler.setEndTime(actualEndTime);
//        inflowTimeSeriesAPIResponseHandler.setEndDate(actualEndDate);
//        inflowTimeSeriesAPIResponseHandler.setEndTime(actualEndTime);
//        
//        dischargeTimeSeriesAPIResponseHandler.fillEventList();
//        volumeTimeSeriesAPIResponseHandler.fillEventList();
//        inflowTimeSeriesAPIResponseHandler.fillEventList();
//        
//        dischargeTimeSeriesAPIResponseHandler.writeFEWObject();
//        volumeTimeSeriesAPIResponseHandler.writeFEWObject();
//        inflowTimeSeriesAPIResponseHandler.writeFEWObject();
        
        Runtime rt = Runtime.getRuntime();

        try {

            // just need to execute the task manager
            Process proc = rt.exec(talsimTaskManagerPath);

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader =
                    new JavaProcessStreamReader(proc.getErrorStream(), "ERROR", pipedOut);

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT");

            // start them
            errorStreamReader.start();
            outputStreamReader.start();

            // fetch errors if there are any
            String errors = "";
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipedIn));) {
                String line = errorReader.readLine();

                while (line != null) {
                    errors = errors.concat(line + lineSeparator);
                    line = errorReader.readLine();
                }
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java proces was interrupted.", e1);
            } finally {
                proc.destroy();
            }

            try {
                // TODO create Observations from Talsim output
                // Create file inputstream from output file
                InputStream outputFileInputStream = new FileInputStream(outputFile);

                TalsimSosConverter talsimSosConverter = new TalsimSosConverter();
                
                talsimSosConverter.insertOutputToSOS(outputFileInputStream, sosURL);
            } catch (Exception e) {
                LOGGER.error("Could not store TALSIM output ijn transactional SOS.", e);
            }

        } catch (IOException e) {
            LOGGER.error("Something went wrong while executing Talsim.", e);
        }

    }

    private boolean clearDirectory(String path) {

        File directory = new File(path);

        if (!directory.exists()) {
            throw new IllegalArgumentException("Directory " + directory.getAbsolutePath() + " does not exist.");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("File " + directory.getAbsolutePath() + " is not a directory.");
        }

        File[] files = directory.listFiles();

        for (File file : files) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

}
