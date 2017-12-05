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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
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
public class QuakemapAlgorithm extends AbstractAlgorithm {

    private SimpleFeatureCollection output;

    private static Logger LOGGER = LoggerFactory.getLogger(QuakemapAlgorithm.class);

    public SimpleFeatureCollection input;

    @Execute
    public void produceQuakemap() {

    }

    private Geometry runBuffer(Geometry a, double width) {
        Geometry buffered = null;

        try {
            buffered = a.buffer(width);
            return buffered;
        } catch (RuntimeException ex) {
            // simply eat exceptions and report them by returning null
        }
        return null;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {
        
        input = (SimpleFeatureCollection) ((GTVectorDataBinding)inputData.get("input").get(0)).getPayload();
        
        double i = 1;
        String uuid = UUID.randomUUID().toString();
        List<SimpleFeature> featureList = new ArrayList<>();
        SimpleFeatureType featureType = null;

        for (SimpleFeatureIterator ia = (SimpleFeatureIterator)input.features(); ia.hasNext();) {

            /**
             * ******************
             */
            SimpleFeature feature = ia.next();
            
            Property magnitudeProperty = feature.getProperty("magnitude");
            
            double magnitude;
            
            if(magnitudeProperty != null){
                magnitude = (double) magnitudeProperty.getValue();
            }else{
                
                int randomInt = new Random().nextInt(9);
                
                magnitude = randomInt == 0 ? 2 : randomInt;
                
                LOGGER.warn("No Magnitude property found. Returning input features.");
                output = input;
                return null;
            }
            
            double width = 0.01;
            
            double floor = 4.0;
            
            if(magnitude <= 5.0){
                floor = 1;
            }else if((5.0 < magnitude) && (magnitude <= 7.0)){
                floor = 3;
            }
            
            for (double counter = magnitude; counter >= floor; counter--) {
                
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
                
                width = width * 4;
                
                LOGGER.info("Magnitude: " + counter + ", width " + width);
            }
        }
        output = GTHelper.createSimpleFeatureCollectionFromSimpleFeatureList(featureList);
        
        Map<String, IData> result = new HashMap<String, IData>(1);
        
        result.put("output", new GTVectorDataBinding(output));
        
        return result;
    }

    @Override
    public List<String> getErrors() {
        return null;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        return GTVectorDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return GTVectorDataBinding.class;
    }

}
