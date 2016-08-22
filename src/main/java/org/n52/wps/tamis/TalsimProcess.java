package org.n52.wps.tamis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.io.data.binding.complex.GenericXMLDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Algorithm(
        version = "0.01")
public class TalsimProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(TalsimProcessTest.class);

    private final String lineSeparator = System.getProperty("line.separator");

    private List<XmlObject> complexInput;

    private List<String> literalInput;

    private XmlObject complexOutput;

    private String literalOutput;

    @ComplexDataOutput(
            identifier = "complexOutput", binding = GenericXMLDataBinding.class)
    public XmlObject getComplexOutput() {
        return complexOutput;
    }

    @LiteralDataOutput(
            identifier = "literalOutput")
    public String getLiteralOutput() {
        return literalOutput;
    }

    @ComplexDataInput(
            binding = GenericXMLDataBinding.class, identifier = "complexInput", minOccurs = 0, maxOccurs = 1)
    public void setComplexInput(List<XmlObject> complexInput) {
        this.complexInput = complexInput;
    }

    @LiteralDataInput(
            identifier = "literalInput", minOccurs = 0, maxOccurs = 1)
    public void setLiteralInput(List<String> literalInput) {
        this.literalInput = literalInput;
    }

    @Execute
    public void run() {

        String baseDir = "D:/52n/Projekte/Laufend/TAMIS/talsim-ng-inkl-doku/talsim-ng/customers/wv/applications/TaskMgr";

        Runtime rt = Runtime.getRuntime();

        try {

            Process proc = rt.exec(baseDir + File.separator + "SydroTaskMgr.exe");

            PipedOutputStream pipedOut = new PipedOutputStream();

            PipedInputStream pipedIn = new PipedInputStream(pipedOut);

            // attach error stream reader
            JavaProcessStreamReader errorStreamReader = new JavaProcessStreamReader(proc.getErrorStream(), "ERROR");

            // attach output stream reader
            JavaProcessStreamReader outputStreamReader = new JavaProcessStreamReader(proc.getInputStream(), "OUTPUT", pipedOut);

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
            
//            // fetch errors if there are any
//            String log = "";
//            try (BufferedReader logReader = new BufferedReader(new InputStreamReader(pipedIn));) {
//                String line = logReader.readLine();
//                
//                while (line != null) {
//                    log = log.concat(line + lineSeparator);
//                    line = logReader.readLine();
//                }
//            }

            try {
                proc.waitFor();
            } catch (InterruptedException e1) {
                LOGGER.error("Java proces was interrupted.", e1);
            } finally {
                proc.destroy();
            }

        } catch (IOException e) {
            LOGGER.error("Something went wrong while executing TalSIM.", e);        
        }

    }

}
