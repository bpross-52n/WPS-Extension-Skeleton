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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.n52.project.riesgos.earthquakesimulation.EarthquakeSimulationDBConnector;
import org.n52.project.riesgos.util.PropertyUtil;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;

/**
 * This process returns isochrones for tsunami waves based on an input
 * epicenter.
 * 
 * @author Benjamin Pross
 *
 */
@Algorithm(
        version = "0.1.0", abstrakt = "This process returns isochrones for tsunami waves based on an input epicenter.",
        title = "GetIsochrones process",
        statusSupported = true, storeSupported = true)
public class GetIsochronesProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(GetIsochronesProcess.class);
    
    private FeatureCollection output;

    private FeatureCollection inputEpicenter;

    @ComplexDataInput(
            identifier = "epicenter", binding = GTVectorDataBinding.class)
    public void setEpicenter(FeatureCollection inputEpicenter) {
        this.inputEpicenter = inputEpicenter;
    }

    @ComplexDataOutput(
            identifier = "isochrones", binding = GTVectorDataBinding.class)
    public FeatureCollection getOutput() {
        return this.output;
    }

    @Execute
    public void run() {

        EarthquakeSimulationDBConnector earthquakeSimulationDBConnector = new EarthquakeSimulationDBConnector();
        
        String username = "";
        String password = "";

        String connectionURL = "";
        
        try {
			PropertyUtil propertyUtil = PropertyUtil.getInstance();
			
			username = propertyUtil.getUserName();
			password = propertyUtil.getPassword();
			connectionURL = propertyUtil.getConnectionURL();
			
		} catch (IOException e1) {
			LOGGER.error("Could not get connection properties.");
			return;
		}

        try {
            earthquakeSimulationDBConnector.connectToDB(connectionURL, username, password);
        } catch (SQLException e) {
            LOGGER.error("Could not connect to database.", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Could not find postgresql driver class.");
        }

        String id = (String) inputEpicenter.features().next().getProperty("scenario_id").getValue();
        
        try {
            List<String> isochronesList = earthquakeSimulationDBConnector.getIsochronesChile(id);
            
            output = createFeatures(isochronesList);
            
        } catch (SQLException | FactoryException e) {
            LOGGER.error("Could not get icochrones for id: " + id, e);
        }
    }
    
    public SimpleFeatureCollection createFeatures(List<String> geoJSONList) throws NoSuchAuthorityCodeException, FactoryException{

        
        List<Feature> featureList = new ArrayList<>();
        
        for (Iterator<String> it = geoJSONList.iterator(); it.hasNext();) {
            
            String geometryString = it.next();
            
            FeatureCollection<?,?> features;
            try {
                features = new FeatureJSON().readFeatureCollection(new ByteArrayInputStream(geometryString.getBytes()));
            } catch (Exception e) {
                LOGGER.error("Could not parse geometry: " + geometryString, e);
                continue;
            }
            
            featureList.addAll(Arrays.asList(features.toArray(new Feature[]{})));
        }
        
        return createCorrectFeatureCollection(featureList);
    }

    private SimpleFeatureCollection createCorrectFeatureCollection(List<Feature> featureList) throws NoSuchAuthorityCodeException, FactoryException {

        //need mapping between textual categories and values

        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");

        List<SimpleFeature> simpleFeatureList = new ArrayList<SimpleFeature>();
        SimpleFeatureType featureType = null;
        Iterator<Feature> iterator = featureList.iterator();
        String uuid = UUID.randomUUID().toString();
        int i = 0;
        while(iterator.hasNext()){
            SimpleFeature feature = (SimpleFeature) iterator.next();
            if(i==0){
                featureType = GTHelper.createFeatureType(feature.getProperties(), (Geometry)feature.getDefaultGeometry(), uuid, crs);
                QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
                SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());
            }
            SimpleFeature resultFeature = GTHelper.createFeature("ID"+i, (Geometry)feature.getDefaultGeometry(), featureType, feature.getProperties());

            simpleFeatureList.add(resultFeature);
            i++;
        }

        ListFeatureCollection resultFeatureCollection = new ListFeatureCollection(featureType, simpleFeatureList);
        return resultFeatureCollection;

    }

}
