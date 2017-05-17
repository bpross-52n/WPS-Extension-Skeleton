package org.n52.wps.tamis;

import java.io.InputStream;

import org.junit.Test;
import org.n52.wps.webapp.common.AbstractITClass;

public class TalsimProcessTest  extends AbstractITClass{

    @Test
    public void TestTalsimProcess() {
        
        TalsimProcessHelper talsimProcessHelper = new TalsimProcessHelper();

        String talsimFEWStoTALSIMDataPath = "d:/tmp/";
        InputStream dischargeInputStream = getClass().getResourceAsStream("dischargeInput");
        InputStream inflowInputStream = getClass().getResourceAsStream("volumeInput");
        InputStream volumeInputStream = getClass().getResourceAsStream("dischargeInput");
        
        talsimProcessHelper.setDischargeInputStream(dischargeInputStream);
        talsimProcessHelper.setInflowInputStream(inflowInputStream);
        talsimProcessHelper.setTalsimFEWStoTALSIMDataPath(talsimFEWStoTALSIMDataPath);
        talsimProcessHelper.setVolumeInputStream(volumeInputStream);
        
        talsimProcessHelper.createTalsimInputs();
       
    }

}
