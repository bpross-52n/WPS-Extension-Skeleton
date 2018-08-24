package org.n52.wps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.xmlbeans.impl.common.IOUtil;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.n52.project.riesgos.GetEpicentersProcess;
import org.n52.project.riesgos.GetIsochronesProcess;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.generator.GTBinDirectorySHPGenerator;
import org.n52.wps.io.datahandler.generator.GTBinZippedSHPGenerator;
import org.n52.wps.webapp.common.AbstractITClass;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;

public class CreateFeatureTest extends AbstractITClass {

    @Test
    public void testCreateFeatures() throws IOException {

        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("output.txt"), "UTF-8"));

        String id = "92352";

        GetIsochronesProcess process = new GetIsochronesProcess();

        SimpleFeatureType featureType = process.createFeatureType(id);

//        GTBinDirectorySHPGenerator generator = new GTBinDirectorySHPGenerator();
//
//        generator.truncateAttributeNames(featureType);
        
        Map<String, String> timestampIsochroneMap = new HashMap<>();

        String line = null;

        while ((line = bufferedReader.readLine()) != null) {
            String[] splitLineArray = line.split(";");
            timestampIsochroneMap.put(splitLineArray[0], splitLineArray[1]);
        }

        List<SimpleFeature> featureList = process.createFeatures(timestampIsochroneMap, featureType, id);

        for (SimpleFeature simpleFeature : featureList) {
//            generator.truncatePropertyNames(simpleFeature.getProperties());
            System.out.println(simpleFeature.getID());
            System.out.println(simpleFeature.getAttribute("arrival_time"));
        }
        //
         SimpleFeatureCollection simpleFeatureCollection = new
         ListFeatureCollection(featureType, featureList);
        
         InputStream in = new GTBinZippedSHPGenerator().generateStream(new
         GTVectorDataBinding(simpleFeatureCollection),
         "application/x-zipped-shp", null);
        
         File outputZip = File.createTempFile("" + System.currentTimeMillis(),
         ".zip");
        
         OutputStream out = new FileOutputStream(outputZip);
        
         IOUtil.copyCompletely(in, out);
        
         System.out.println(outputZip.getAbsolutePath());

    }

    private Map<String, String> attributeNameMap = new HashMap<>();

    @Test
    public void testTruncateNames() {

        List<String> names = new ArrayList<>();

        names.add("population_min");
        names.add("population_max");
        names.add("population_mean");
        names.add("test");

        for (String attributeName : names) {
            if (attributeName.length() > 10) {
                // truncate
                String newAttributeName = attributeName.substring(0, 10);
                newAttributeName = new GTBinDirectorySHPGenerator().checkNames(attributeName, newAttributeName, attributeNameMap);
                System.out.println(newAttributeName);
                attributeNameMap.put(attributeName, newAttributeName);
            }
        }
        
        for (String string : attributeNameMap.keySet()) {
            System.out.println(string + " " + attributeNameMap.get(string));
        }
    }
    
    @Test
    public void testGetEpicenters() throws IOException{
        
        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("epicenters.txt"), "UTF-8"));

        GetEpicentersProcess process = new GetEpicentersProcess();

        SimpleFeatureType featureType = process.createFeatureType(UUID.randomUUID().toString().substring(0, 5));
        
        Map<String, Coordinate> idCoordinateMap = new HashMap<>();

        String line = null;

        while ((line = bufferedReader.readLine()) != null) {
            String[] splitStringArray = line.split(",");
            
            String id = splitStringArray[0];
            
            double longitude = Double.parseDouble(splitStringArray[1]);
            double latitude = Double.parseDouble(splitStringArray[2]);
            
            idCoordinateMap.put(id, new Coordinate(longitude, latitude));
        }
        
        FeatureCollection<?, ?> simpleFeatureCollection = process.createFeatureCollection(idCoordinateMap, featureType);
        
        InputStream in = new GTBinZippedSHPGenerator().generateStream(new
        GTVectorDataBinding(simpleFeatureCollection),
        "application/x-zipped-shp", null);
       
        File outputZip = File.createTempFile("" + System.currentTimeMillis(),
        ".zip");
       
        OutputStream out = new FileOutputStream(outputZip);
       
        IOUtil.copyCompletely(in, out);
       
        System.out.println(outputZip.getAbsolutePath());
    }

}
