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

import static org.n52.wps.server.AbstractAnnotatedAlgorithm.LOGGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Produce a simple quakemap based on coordinate and magnitude
 * 
 * @author Benjamin Pross
 *
 */
@Algorithm(
        version = "1.0", abstrakt = "Produce a simple quakemap based on coordinate and magnitude",
        title = "Quakemap algoritm", statusSupported = false, storeSupported = false)
public class QuakemapAlgorithm extends AbstractAnnotatedAlgorithm {

    private FeatureCollection output;

    private static Logger LOGGER = LoggerFactory.getLogger(QuakemapAlgorithm.class);

    @ComplexDataInput(
            identifier = "input", title = "earthquake information",
            abstrakt = "Coordinate and magnitude of an earthquake", binding = GTVectorDataBinding.class)
    public FeatureCollection input;

    @ComplexDataOutput(
            identifier = "output", title = "Buffers with decreasing magnitudes", abstrakt = "Buffers with decreasing magnitudes",
            binding = GTVectorDataBinding.class)
    public FeatureCollection getOutput() {
        return this.output;
    }

    @Execute
    public void produceQuakemap() {

        double i = 0;
        int totalNumberOfFeatures = input.size();
        String uuid = UUID.randomUUID().toString();
        List<SimpleFeature> featureList = new ArrayList<>();
        SimpleFeatureType featureType = null;
        LOGGER.debug("");
        for (FeatureIterator ia = input.features(); ia.hasNext();) {

            /**
             * ******************
             */
            SimpleFeature feature = (SimpleFeature) ia.next();
            
            Property magnitudeProperty = feature.getProperty("magnitude");
            
            double magnitude;
            
            if(magnitudeProperty != null){
                magnitude = (double) magnitudeProperty.getValue();
            }else{
                
                int randomInt = new Random().nextInt(9);
                
                magnitude = randomInt == 0 ? 1 : randomInt;
            }
            
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Geometry geometryBuffered = runBuffer(geometry, width);

            if (i == 1) {
                CoordinateReferenceSystem crs = feature.getFeatureType().getCoordinateReferenceSystem();
                if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
                    crs = ((CoordinateReferenceSystem) geometry.getUserData());
                }
                featureType = GTHelper.createFeatureType(feature.getProperties(), geometryBuffered, uuid, crs);
                QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
                SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());

            }

            if (geometryBuffered != null) {
                SimpleFeature createdFeature = (SimpleFeature) GTHelper.createFeature("ID" + new Double(i).intValue(),
                        geometryBuffered, (SimpleFeatureType) featureType, feature.getProperties());
                feature.setDefaultGeometry(geometryBuffered);
                featureList.add(createdFeature);
            } else {
                LOGGER.warn("GeometryCollections are not supported, or result null. Original dataset will be returned");
            }
        }
        output = GTHelper.createSimpleFeatureCollectionFromSimpleFeatureList(featureList);

    }

}
