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
import nl.wldelft.fews.pi.TimeSeriesComplexType;
import nl.wldelft.fews.pi.TimeSeriesDocument;

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
        File outputFile = new File(talsimTALSIMtoFEWSDataPath + File.separator + outputFilename);
        
        Runtime rt = Runtime.getRuntime();

        String tmpDirString = "TMP=" + System.getProperty("java.io.tmpdir");

        try {

            // just need to execute the task manager
            Process proc = rt.exec(talsimTaskManagerPath, new String[]{tmpDirString});

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
                
                TalsimToTimeseries talsimToTimeseries = new TalsimToTimeseries();
                
                TimeSeriesDocument timeSeriesDocument = TimeSeriesDocument.Factory.parse(outputFileInputStream);

                for (TimeSeriesComplexType seriesArray : timeSeriesDocument.getTimeSeries().getSeriesArray()) {
                    
                    String parameterId = seriesArray.getHeader().getParameterId();
                    
                    String timeseriesJSONString = talsimToTimeseries.createTimeseriesJSONFromTimeseriesXML(seriesArray);
                    
                    switch (parameterId) {
                    case "1ZU":
                        inflowOutput = timeseriesJSONString;
                        break;
                    case "VOL":
                        volumeOutput = timeseriesJSONString;
                        break;
                    case "WSP":
                        waterlevelOutput = timeseriesJSONString;
                        break;
                    case "QA1":
                        dischargeOutput = timeseriesJSONString;
                        break;
                    case "QH1":
                        spillwayDischargeOutput = timeseriesJSONString;
                        break;
                    default:
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Could create TimeSeries from Talsim outputs.", e);
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
