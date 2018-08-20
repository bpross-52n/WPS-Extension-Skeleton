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
package org.n52.project.riesgos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.n52.project.riesgos.earthquakesimulation.EarthquakeSimulationDBConnector;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * This process returns isochrones for tsunami waves based on an input
 * epicenter.
 * 
 * @author Benjamin Pross
 *
 */
@Algorithm(
        version = "0.1", abstrakt = "This process returns isochrones for tsunami waves based on an input epicenter.",
        title = "GetIsochrones process",
        statusSupported = false, storeSupported = false)
public class GetIsochronesProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(GetIsochronesProcess.class);
    
    private FeatureCollection<?, ?> output;

    private FeatureCollection<?, ?> inputEpicenter;

    @ComplexDataInput(
            identifier = "epicenter", binding = GTVectorDataBinding.class)
    public void setEpicenter(FeatureCollection<?, ?> inputEpicenter) {
        this.inputEpicenter = inputEpicenter;
    }

    @ComplexDataOutput(
            identifier = "isochrones", binding = GTVectorDataBinding.class)
    public FeatureCollection<?, ?> getOutput() {
        return this.output;
    }

    @Execute
    public void run() {

        EarthquakeSimulationDBConnector earthquakeSimulationDBConnector = new EarthquakeSimulationDBConnector();
        
        String username = "wave";
        String password = "tsunami";

        String connectionURL = "jdbc:postgresql://postgres6.awi.de:5432/tsunami";

        try {
            earthquakeSimulationDBConnector.connectToDB(connectionURL, username, password);
        } catch (SQLException e) {
            LOGGER.error("Could not connect to database.", e);
        }
        
        Property idProperty = inputEpicenter.features().next().getProperty("id");

        String id = (String) idProperty.getValue();
        
        try {
            Map<String, String> timestampIsochroneMap = earthquakeSimulationDBConnector.getIsochrones(id);
            
            List<SimpleFeature> featureList = new ArrayList<>();
            
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("Feature-"+id);
            builder.setCRS(DefaultGeographicCRS.WGS84);

            builder.add("the_geom", MultiLineString.class);
            builder.add("arrival_time", Integer.class);

            // build the type
            SimpleFeatureType featureType = builder.buildFeatureType();
            
            for (Iterator<String> it = timestampIsochroneMap.keySet().iterator(); it.hasNext();) {

                int i = 1;
                
                String timestamp = it.next();
                String geometryString = timestampIsochroneMap.get(timestamp);
                
                Geometry geometry;
                try {
                    geometry = new WKTReader().read(geometryString);
                } catch (ParseException e) {
                    LOGGER.error("Could not parse geometry: " + geometryString, e);
                    continue;
                }

                if (i == 1) {
                    QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
                    SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());
                }

                if (geometry != null) {
                    SimpleFeature createdFeature = (SimpleFeature) GTHelper.createFeature(id, geometry, (SimpleFeatureType) featureType);
                    createdFeature.setDefaultGeometry(geometry);
                    createdFeature.setAttribute("arrival_time", timestamp);
                    featureList.add(createdFeature);
                } else {
                    LOGGER.warn("GeometryCollections are not supported, or result null. Original dataset will be returned");
                }
                i++;
            }
            
            output = new ListFeatureCollection(featureType, featureList);
            
        } catch (SQLException e) {
            LOGGER.error("Could not get icochrones for id: " + id, e);
        }
    }

}
