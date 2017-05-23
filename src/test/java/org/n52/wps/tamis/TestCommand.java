package org.n52.wps.tamis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.n52.wps.server.grass.util.JavaProcessStreamReader;

public class TestCommand {

    public static void main(String[] args) throws IOException {

        Runtime rt = Runtime.getRuntime();

        String tmpDirString = "TMP=" + System.getProperty("java.io.tmpdir");
        
        // just need to execute the task manager
//        Process proc = rt.exec("cmd.exe /c start");
        Process proc = rt.exec("cmd.exe /c start", new String[]{tmpDirString}, new File("D:/Programme/talsim-ng/customers/wv/applications/TaskMgr"));

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
