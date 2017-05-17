package org.n52.wps.tamis;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.util.Properties;

import org.apache.xmlbeans.XmlObject;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.binding.complex.GenericXMLDataBinding;
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

        File dischargeInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + dischargeFilename);
        File volumeInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + volumeFilename);
        File inflowInputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + inflowFilename);
        File outputFile = new File(talsimFEWStoTALSIMDataPath + File.separator + outputFilename);

        // create discharge input for Talsim
        try {

            FileOutputStream dischargeFileOutputStream = new FileOutputStream(dischargeInputFile);

            FEWObject dischargeFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                    .setLocationId(FEWObject.LocationID.TBEV.toString())
                    .setParameterId(FEWObject.ParameterId.QA1.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());
            try {
                
                new TimeSeriesAPIResponseHandler().setInputStream(Util.connectWithBasicAuth(dischargeInput, userName, password))
                        .setOutputStream(dischargeFileOutputStream).setFEWObject(dischargeFEWObject).handle();
                
            } catch (Exception e) {
                LOGGER.error("Could not fetch timeseries from: " + dischargeInput, e);
            }

//            new TimeSeriesAPIResponseHandler().setInputStream(new ByteArrayInputStream(dischargeInput.getBytes()))
//                    .setOutputStream(dischargeFileOutputStream).setFEWObject(dischargeFEWObject).handle();

            dischargeFileOutputStream.close();

        } catch (Exception e) {
            throw new RuntimeException("Could not create Talsim input file for discharge.");
        }

        // create volume input for Talsim
        try {

            FileOutputStream volumeFileOutputStream = new FileOutputStream(volumeInputFile);

            FEWObject volumeFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                    .setLocationId(FEWObject.LocationID.TBEV.toString())
                    .setParameterId(FEWObject.ParameterId.Volumen.getParameterID()).setUnits(FEWObject.Unit.millionm3.getUnitForTalSIM());
            try {
                
                new TimeSeriesAPIResponseHandler().setInputStream(Util.connectWithBasicAuth(volumeInput, userName, password))
                        .setOutputStream(volumeFileOutputStream).setFEWObject(volumeFEWObject).handle();
                
            } catch (Exception e) {
                LOGGER.error("Could not fetch timeseries from: " + volumeInput, e);
            }

            volumeFileOutputStream.close();

        } catch (Exception e) {
            throw new RuntimeException("Could not create Talsim input file for volume.");
        }

        // create inflow input for Talsim
        try {

            FileOutputStream inflowFileOutputStream = new FileOutputStream(inflowInputFile);

            FEWObject inflowFEWObject = new FEWObject().setType(FEWObject.Types.instantaneous.toString())
                    .setLocationId(FEWObject.LocationID.EBEV.toString())
                    .setParameterId(FEWObject.ParameterId.Zufluss.getParameterID()).setUnits(FEWObject.Unit.m3persecond.getUnitForTalSIM());
            try {
                
                new TimeSeriesAPIResponseHandler().setInputStream(Util.connectWithBasicAuth(inflowInput, userName, password))
                        .setOutputStream(inflowFileOutputStream).setFEWObject(inflowFEWObject).handle();
                
            } catch (Exception e) {
                LOGGER.error("Could not fetch timeseries from: " + inflowInput, e);
            }

            inflowFileOutputStream.close();

        } catch (Exception e) {
            throw new RuntimeException("Could not create Talsim input file for inflow.");
        }

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
