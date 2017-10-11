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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple mockup algorithm doing nothing special. This time in an
 * {@link Annotation} version.
 * 
 * @author matthes rieke
 *
 */
@Algorithm(
        version = "1.0",
        abstrakt = "Fuses statistical data from https://www.regionalstatistik.de/ with polygons from http://sg.geodatenzentrum.de/wfs_vg250.",
        title = "Fusion algorithm",
        statusSupported = true, storeSupported = true)
public class FusionAlgorithm extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(FusionAlgorithm.class);

    private FeatureCollection data;
    
    private FeatureCollection result;

    @ComplexDataInput(identifier = "data", binding = GTVectorDataBinding.class)
    public void setData(FeatureCollection data) {
        this.data = data;
    }

    @LiteralDataInput(
            identifier = "year", title = "The year of the statistics", allowedValues={"2015", "2014", "2013"})
    public String year;
    
    @LiteralDataInput(
            identifier = "code", title = "The code of the statistics", allowedValues={"45412-01-02-4"})
    public String code;

    @ComplexDataOutput(
            identifier = "result", binding = GTVectorDataBinding.class)
    public FeatureCollection getResult() {
        return result;
    }

    @Execute
    public void runFusion() throws ExceptionReport {

        LOGGER.info("Got year: " + year);
        LOGGER.info("Got code: " + code);
        LOGGER.info("Got features: " + data.size());
        
        Map<Integer, DBEntry> map;
        try {
            map = FusionUtil.createMap(year, code);
        } catch (IOException e) {
            LOGGER.error("Could not create map.", e);
            throw new ExceptionReport("Could not initialize statistics map.", ExceptionReport.NO_APPLICABLE_CODE);
        }
        
        FeatureIterator<?> featureIterator = data.features();
        
        SimpleFeatureType featureType = FusionUtil.createSimpleFeatureTypeWithAdditionalProperties(data);

        SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(featureType);

        List<SimpleFeature> featureList = new ArrayList<>();

        while (featureIterator.hasNext()) {

            Feature feature = featureIterator.next();

            Collection<Property> properties = feature.getProperties();
            
            Collection<Object> newProperties = new ArrayList<>();
            
            Iterator<Property> propertyIterator = properties.iterator();
            
            while (propertyIterator.hasNext()) {
                Property property = (Property) propertyIterator.next();
                if(!property.getName().getLocalPart().equals("boundedBy")){
                    newProperties.add(property.getValue());
                }
            }
            
            simpleFeatureBuilder.addAll(newProperties.toArray());

            Property rsProperty = feature.getProperty("RS");
            
            Object rs = rsProperty.getValue();

            DBEntry dbEntry = map.get(Integer.parseInt((String) rs));
            
            simpleFeatureBuilder.addAll(dbEntry.getAttributeArray());

            SimpleFeature newFeature = simpleFeatureBuilder.buildFeature(feature.getIdentifier().getID());
            
            newFeature.setDefaultGeometry(((SimpleFeature)feature).getDefaultGeometry());
            
            featureList.add(newFeature);
        }

        LOGGER.info("FeatureList size: " + featureList.size());

        result = new ListFeatureCollection(featureType, featureList);
    }

}
