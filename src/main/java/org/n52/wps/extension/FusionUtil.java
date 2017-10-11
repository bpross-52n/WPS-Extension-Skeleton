package org.n52.wps.extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FusionUtil {

    private static final Logger logger = LoggerFactory.getLogger(FusionUtil.class);
    
    public static String[] attributeNames = new String[] { "Geoeffnete_Beherbergungsbetriebe", "Angebotene_Gaestebetten",
            "Gaesteuebernachtungen", "Gaesteankuenfte" };

    public static Map<Integer, DBEntry> createMap(String year, String code) throws IOException {
        
        String pathToCSVFile = System.getProperty("user.home") + File.separator + "unece-workshop" + File.separator + year + File.separator + code + ".csv";
        
        logger.info("Path to csv file: " + pathToCSVFile);
        
        Map<Integer, DBEntry> result = new HashMap<>();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(pathToCSVFile)));

        String line = null;

        while ((line = bufferedReader.readLine()) != null) {

            // if(attributeNames == null){
            // getAttributeNames(line);
            // continue;
            // }

            DBEntry entry = createDBEntry(line);

            if (entry != null) {
                result.put(entry.getDg(), entry);
            }
        }

        logger.info("Size of map: " + result.size());
        
        bufferedReader.close();

        return result;
    }

    // private static void getAttributeNames(String line) {
    // String[] stringArray = line.split(";");
    // if(stringArray.length == 8 && !stringArray[4].equals("")){
    //
    // }
    //
    // }

    private static DBEntry createDBEntry(String line) {
        String[] stringArray = line.split(";");
        if (stringArray.length == 7) {
            int dg; 
            
            try {
                dg = Integer.parseInt(stringArray[1]);
            } catch (Exception e) {
                logger.info("Could not parse Integer:" + stringArray[1]);
                return null;
            }
            
            if(dg > 1000 && dg < 17000){
                
//                if(stringArray[3].equals("Insgesamt")){
                    return createDBEntry(dg,stringArray);
//                }
                
            }
        }
        return null;
    }
    
    private static DBEntry createDBEntry(int dg, String[] stringArray) {        
        return new DBEntry(dg, stringArray[2], Arrays.copyOfRange(stringArray, 3, 7));
    }

    public static SimpleFeatureType createSimpleFeatureTypeWithAdditionalProperties(FeatureCollection<?, ?> featureCollection){
        
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
        
        return featureType;
        
    }
    
}