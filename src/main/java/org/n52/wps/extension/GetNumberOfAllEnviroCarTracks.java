package org.n52.wps.extension;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;

import com.fasterxml.jackson.databind.ObjectMapper;

@Algorithm(version = "1.0.0")
public class GetNumberOfAllEnviroCarTracks extends AbstractAnnotatedAlgorithm{
	
	private static Logger LOGGER = Logger.getLogger(GetNumberOfAllEnviroCarTracks.class);
	private int numberOfAllEnviroCarTracks;
	private String serverURL;

	@LiteralDataInput(identifier = "serverURL", minOccurs = 1)
	public void setFuelType(String serverURL) {
		this.serverURL = serverURL;
	}
	
	@LiteralDataOutput(identifier = "numberOfAllEnviroCarTracks")
	public int getFuelPrice() {
		return numberOfAllEnviroCarTracks;
	}
	
	@Execute
	public void getNumberOfAllTracks(){		
		
		try {
			getNumberOfTracks(1);
		} catch (Exception e) {
			LOGGER.error(e);
		}
		
	}
	
	private void getNumberOfTracks(int page) throws Exception{
		
		URL url = new URL(serverURL + 
				"?limit=100&page=" + page);

		URLConnection connection = url.openConnection();
		
		ObjectMapper objMapper = new ObjectMapper();

		Map<?, ?> map = objMapper.readValue(connection.getInputStream(), Map.class);

		for (Object o : map.keySet()) {
			Object entry = map.get(o);
			
			if(entry instanceof ArrayList<?>){
				numberOfAllEnviroCarTracks += ((ArrayList<?>)entry).size();
			}
		}
		
		Map<String, List<String>> responseMap = connection.getHeaderFields();

		List<String> values = responseMap.get("Link");

		boolean isMoreTracks = false;
		boolean relNext = false;
		boolean relLast= false;
		
		for (int i = 0; i < values.size(); i++) {
			String o = values.get(i);
			String[] params = o.split(";");
			for (String string : params) {
				if(string.equals("rel=next")){
					relNext = true;
				}else if(string.equals("rel=last")){
					relLast = true;
				}
			}
		}
		
		if(relLast || relNext){
			isMoreTracks = true;
		}
		
		if(isMoreTracks){
			getNumberOfTracks(page + 1);
		}
	}
	
}
