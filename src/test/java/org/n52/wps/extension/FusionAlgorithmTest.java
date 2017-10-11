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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.generator.GML2BasicGenerator;
import org.n52.wps.io.datahandler.generator.GTBinZippedSHPGenerator;
import org.n52.wps.io.datahandler.parser.GML2BasicParser;
import org.n52.wps.io.test.datahandler.AbstractTestCase;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

public class FusionAlgorithmTest extends AbstractTestCase<GML2BasicParser> {

    @Test
    public void testAlgo() throws ExceptionReport {

        String pathToCSVFile = "D:/Chaos-Folder/45412-02-01-4.csv";
        try {
            // URL wfsRequest = new
            // URL("http://sg.geodatenzentrum.de/wfs_vg250?request=GetFeature&service=wfs&version=1.0.0&TypeName=vg250:vg250_krs&maxFeatures=10&outputFormat=application/gml%2Bxml;%20version=3.2");
            URL wfsRequest = new URL(
                    "http://sg.geodatenzentrum.de/wfs_vg250?request=GetFeature&service=wfs&version=1.0.0&TypeName=vg250:vg250_krs&maxFeatures=10");

            GTVectorDataBinding gtVectorDataBinding = dataHandler.parse(wfsRequest.openStream(), "", "");

            FeatureCollection<?, ?> featureCollection = gtVectorDataBinding.getPayload();

            LOGGER.info("Size of FeatureCollection: " + featureCollection.size());
            Map<Integer, DBEntry> map = FusionUtil.createMap(pathToCSVFile);

            LOGGER.info("Map size: " + map.size());

            LOGGER.info(map.entrySet().iterator().next().toString());

            FeatureIterator<?> featureIterator = featureCollection.features();

            FeatureType schema = featureCollection.getSchema();

            AttributeTypeBuilder attributeTypeBuilder = new AttributeTypeBuilder();

            attributeTypeBuilder.setName(FusionUtil.attributeNames[0]);

            attributeTypeBuilder.setBinding(String.class);

            PropertyDescriptor propertyDescriptor = attributeTypeBuilder.buildDescriptor(FusionUtil.attributeNames[0]);

            Collection<PropertyDescriptor> propertyDescriptors = schema.getDescriptors();

            Collection<PropertyDescriptor> newPropertyDescriptors = new ArrayList<>();

            newPropertyDescriptors.addAll(propertyDescriptors);

            newPropertyDescriptors.add(propertyDescriptor);

            Feature firstFeature = featureCollection.features().next();

            Collection<Property> originalProperties = firstFeature.getProperties();

            SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();

            for (Property property : originalProperties) {
                if(property.getName().getLocalPart().equals("boundedBy")){
                    continue;
                }
                simpleFeatureTypeBuilder.add((AttributeDescriptor) property.getDescriptor());
            }

            simpleFeatureTypeBuilder.add(FusionUtil.attributeNames[0], Long.class);
            simpleFeatureTypeBuilder.add(FusionUtil.attributeNames[1], Long.class);
            simpleFeatureTypeBuilder.add(FusionUtil.attributeNames[2], Long.class);
            simpleFeatureTypeBuilder.add(FusionUtil.attributeNames[3], Long.class);
            
            simpleFeatureTypeBuilder
                    .setDefaultGeometry(firstFeature.getDefaultGeometryProperty().getName().getLocalPart());

            simpleFeatureTypeBuilder.setName(featureCollection.getSchema().getName());

            simpleFeatureTypeBuilder.setCRS(featureCollection.getBounds().getCoordinateReferenceSystem());
            
            SimpleFeatureType featureType = simpleFeatureTypeBuilder.buildFeatureType();

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

//                Property xyz = new AttributeBuilder(attributeFactory).buildSimple(id, value)
                
                Object rs = rsProperty.getValue();

                LOGGER.info("RS:" + rs);

                DBEntry dbEntry = map.get(Integer.parseInt((String) rs));
                
                simpleFeatureBuilder.addAll(dbEntry.getAttributeArray());

                SimpleFeature newFeature = simpleFeatureBuilder.buildFeature(feature.getIdentifier().getID());
                
                newFeature.setDefaultGeometry(((SimpleFeature)feature).getDefaultGeometry());
                
                featureList.add(newFeature);
            }

            LOGGER.info("FeatureList size: " + featureList.size());

            SimpleFeatureCollection outputCollection = new ListFeatureCollection(featureType, featureList);
            
            GTBinZippedSHPGenerator shpGenerator = new GTBinZippedSHPGenerator();
            InputStream in = shpGenerator.generateStream(new GTVectorDataBinding(outputCollection), "", "");
//            GML2BasicGenerator gml2BasicGenerator = new GML2BasicGenerator();
//            InputStream in = gml2BasicGenerator.generateStream(new GTVectorDataBinding(outputCollection), "", "");

            File tmpFile = File.createTempFile("fusionTest", ".zip");

            LOGGER.info("Tmp file path: " + tmpFile.getAbsolutePath());

            OutputStream fileOut = new FileOutputStream(tmpFile);

            IOUtils.copy(in, fileOut);

        } catch (Exception e1) {
            LOGGER.error(e1.getMessage());
        }
    }

    @Override
    protected void initializeDataHandler() {
        dataHandler = new GML2BasicParser();

    }

}
