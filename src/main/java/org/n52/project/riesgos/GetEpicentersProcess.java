package org.n52.project.riesgos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.n52.project.riesgos.earthquakesimulation.EarthquakeSimulationDBConnector;
import org.n52.wps.io.GTHelper;
import org.n52.wps.io.SchemaRepository;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.bbox.BoundingBoxData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractObservableAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GetEpicentersProcess extends AbstractObservableAlgorithm{

    private static Logger LOGGER = LoggerFactory.getLogger(GetEpicentersProcess.class);
            
    List<String> errors = new ArrayList<String>();
    
    public GetEpicentersProcess() {
        super("org.n52.project.riesgos.GetEpicentersProcess");
    }
    
    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {        
        
        List<IData> bboxDataList = inputData.get("input-boundingbox");
        
        BoundingBoxData boundingBoxData = (BoundingBoxData) bboxDataList.get(0);
        
        String minLon = boundingBoxData.getLowerCorner()[1] +"";
        String maxLon = boundingBoxData.getUpperCorner()[1] +"";
        String minLat = boundingBoxData.getLowerCorner()[0] +"";
        String maxLat = boundingBoxData.getUpperCorner()[0] +"";
        
        EarthquakeSimulationDBConnector earthquakeSimulationDBConnector = new EarthquakeSimulationDBConnector();
        
        String username = "wave";
        String password = "tsunami";

        String connectionURL = "jdbc:postgresql://postgres6.awi.de:5432/riesgos";

        try {
            earthquakeSimulationDBConnector.connectToDB(connectionURL, username, password);
        } catch (SQLException e) {
            LOGGER.error("Could not connect to database.", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Could not find postgresql driver class.");
        }
        
        Map<String, Coordinate> idCoordinateMap = new HashMap<String, Coordinate>();
        
        try {
            idCoordinateMap = earthquakeSimulationDBConnector.getEpicenterIDsChile(minLon, maxLon, minLat, maxLat);
        } catch (SQLException e) {
            LOGGER.error("Could not create id coordinate map.", e);
            throw new ExceptionReport("Could not create id coordinate map.", ExceptionReport.NO_APPLICABLE_CODE);
        }
        
        SimpleFeatureCollection resultCollection = createFeatureCollection(idCoordinateMap, createFeatureType(UUID.randomUUID().toString().substring(0, 5)));
        
        Map<String, IData> result = new HashMap<>();
        
        result.put("epicenters", new GTVectorDataBinding(resultCollection));
        
        return result;
    }
    
    public SimpleFeatureCollection createFeatureCollection(Map<String, Coordinate> idCoordinateMap, SimpleFeatureType featureType){
        
        List<SimpleFeature> featureList = new ArrayList<>();

        int i = 1;
        
        for (Iterator<String> it = idCoordinateMap.keySet().iterator(); it.hasNext();) {
            
            String id = it.next();
            Coordinate coordinate = idCoordinateMap.get(id);
            
            Point point = new GeometryFactory().createPoint(coordinate);
            
            if (i == 1) {
                QName qname = GTHelper.createGML3SchemaForFeatureType(featureType);
                SchemaRepository.registerSchemaLocation(qname.getNamespaceURI(), qname.getLocalPart());
            }
                SimpleFeature createdFeature = (SimpleFeature) GTHelper.createFeature("" + i, point, featureType);
                createdFeature.setDefaultGeometry(point);
                createdFeature.setAttribute("scenario_id", id);
                featureList.add(createdFeature);
            
            i++;
        }
        
        return GTHelper.createSimpleFeatureCollectionFromSimpleFeatureList(featureList);
        
    }
    
    public SimpleFeatureType createFeatureType(String id){
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        String namespace = "http://www.52north.org/"+id;
        Name name = new NameImpl(namespace, "Feature-"+id);
        builder.setName(name);
        builder.setCRS(DefaultGeographicCRS.WGS84);

        builder.add("the_geom", Point.class);
        builder.add("scenario_id", String.class);

        return builder.buildFeatureType();
        
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        return BoundingBoxData.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return GTVectorDataBinding.class;
    }
    
    @Override
    public ProcessDescription getDescription() {
        // TODO Auto-generated method stub
        return super.getDescription();
    }

}
