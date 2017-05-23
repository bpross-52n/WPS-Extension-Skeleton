package org.n52.wps.tamis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;
import org.n52.wps.server.grass.util.JavaProcessStreamReader;
import org.n52.wps.webapp.common.AbstractITClass;

public class TalsimProcessTest extends AbstractITClass {

    @Test
    public void TestTalsimProcess() {

        TalsimProcessHelper talsimProcessHelper = new TalsimProcessHelper();

        String talsimFEWStoTALSIMDataPath = "d:/tmp/";
        InputStream dischargeInputStream = getClass().getResourceAsStream("dischargeInput");
        InputStream inflowInputStream = getClass().getResourceAsStream("inflowInput");
        InputStream volumeInputStream = getClass().getResourceAsStream("volumeInput");

        talsimProcessHelper.setDischargeInputStream(dischargeInputStream);
        talsimProcessHelper.setInflowInputStream(inflowInputStream);
        talsimProcessHelper.setTalsimFEWStoTALSIMDataPath(talsimFEWStoTALSIMDataPath);
        talsimProcessHelper.setVolumeInputStream(volumeInputStream);

        talsimProcessHelper.createTalsimInputs();

    }

    @Test
    public void testExecuteTaskManager() throws IOException {

        Runtime rt = Runtime.getRuntime();

        // just need to execute the task manager
        Process proc = rt.exec("cmd.exe /c start command");

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
                errors = errors.concat(line + "\n");
                line = errorReader.readLine();
            }
        }

        try {
            proc.waitFor();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        } finally {
            proc.destroy();
        }

    }

}
