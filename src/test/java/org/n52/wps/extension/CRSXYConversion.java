package org.n52.wps.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.datahandler.parser.GML3BasicParser;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class CRSXYConversion {

	public CRSXYConversion(){
		
		File f1 = new File(this.getClass().getProtectionDomain().getCodeSource()
				.getLocation().getFile());
		
		String testFilePath = f1.getParentFile().getParent() + "/src/test/resources/XtraServerGetFeature.gml";
		
		try {
			testFilePath = URLDecoder.decode(testFilePath, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		InputStream input = null;
		
		try {
			input = new FileInputStream(new File(testFilePath));
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}

		GTVectorDataBinding theBinding = new GML3BasicParser().parse(input,
				"text/xml; subtype=gml/3.1.1",
				"http://schemas.opengis.net/gml/3.1.1/base/feature.xsd");
		
		FeatureCollection<?, ?> collection = theBinding.getPayload();
		
		FeatureIterator<?> iterator = collection.features();
		
		ReferencedEnvelope bounds = collection.getBounds();
		
		if(bounds != null){
			
			System.out.println("Bounds: " + bounds.getLowerCorner() + " " + bounds.getUpperCorner());
			
		}else{
			System.out.println("Bounds is null");
		}
		
		GeometryFactory geomFac = new GeometryFactory();
		
		while (iterator.hasNext()) {
			SimpleFeature simpleFeature = (SimpleFeature) iterator.next();
			
			Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
			
			reverseGeometry(geom);
			
//			Polygon newPolygon = null;
//			
//			if(geom instanceof Polygon){
//				
//				Polygon polygon = (Polygon)geom;
//				
//				Coordinate[] coordinates = polygon.getCoordinates();
//				ArrayList<Coordinate> newCoordinates = new ArrayList<Coordinate>();				
//				
//				for (Coordinate coordinate : coordinates) {
//					
//					System.out.print(coordinate.y + " " + coordinate.x + " ");
//					/*
//					 * switch x and y
//					 */
//					newCoordinates.add(new Coordinate(coordinate.y, coordinate.x));					
//				}		
//				
//				newPolygon = new Polygon(geomFac.createLinearRing(newCoordinates.toArray(new Coordinate[]{})), null, geomFac);
//			}
//			if(newPolygon != null){
//				simpleFeature.setDefaultGeometry(newPolygon);
//			}else{
//				System.err.println("The modified polygon is null, can not add it as default geometry, continuing...");
//			}
		}		
		
	}	
	
	private void reverseGeometry(Geometry the_geom){
	
		if(the_geom instanceof Polygon){
			
			Polygon polygon = (Polygon)the_geom;
			
			Polygon revPolygon = (Polygon) polygon.reverse();
			
			Coordinate[] coordinates = revPolygon.getCoordinates();
			
			ArrayList<Coordinate> newCoordinates = new ArrayList<Coordinate>();				
			
			for (Coordinate coordinate : coordinates) {
				
				System.out.print(coordinate.y + " " + coordinate.x + " ");
				/*
				 * switch x and y
				 */
				newCoordinates.add(new Coordinate(coordinate.y, coordinate.x));					
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CRSXYConversion();
	}

}
