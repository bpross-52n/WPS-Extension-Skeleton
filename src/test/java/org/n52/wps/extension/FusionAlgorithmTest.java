/**
 * Copyright (C) 2013
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.wps.extension;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;
import org.n52.wps.server.ExceptionReport;

public class FusionAlgorithmTest extends AbstractTestCase<GML2BasicParser> {

    @Test
    public void testAlgo() throws ExceptionReport {

        try {
            URL wfsRequest = new URL(
                    "http://sg.geodatenzentrum.de/wfs_vg250?request=GetFeature&service=wfs&version=1.0.0&TypeName=vg250:vg250_krs&maxFeatures=10");

            GTVectorDataBinding gtVectorDataBinding = dataHandler.parse(wfsRequest.openStream(), "", "");

            FeatureCollection<?, ?> featureCollection = gtVectorDataBinding.getPayload();

            FusionAlgorithm fusionAlgorithm = new FusionAlgorithm();
            
            fusionAlgorithm.setData(featureCollection);
            fusionAlgorithm.year = "2014";
            fusionAlgorithm.code = "45412-01-02-4";
            
            fusionAlgorithm.runFusion();
            
            FeatureCollection<?,?> outputCollection = fusionAlgorithm.getResult();
            
            assertTrue(outputCollection.size() == 10);

        } catch (Exception e1) {
            LOGGER.error(e1.getMessage());
            fail();
        }
    }

    @Override
    protected void initializeDataHandler() {
        dataHandler = new GML2BasicParser();
    }

}
