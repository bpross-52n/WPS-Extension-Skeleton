package org.n52.project.riesgos.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {

	String userName;
	
	String password;
	
	String connectionURL;
	
	static PropertyUtil instance;
	
	public PropertyUtil() throws IOException {
		
		Properties properties = new Properties();
		
		InputStream in = getClass().getClassLoader().getResourceAsStream("/connection.properties");
		
		properties.load(in);
		
		userName = properties.getProperty("userName");
		password = properties.getProperty("password");
		connectionURL = properties.getProperty("connectionURL");
		
	}
	
	public static PropertyUtil getInstance() throws IOException {
		if(instance == null){
			instance = new PropertyUtil();
		}
		return instance;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getConnectionURL() {
		return connectionURL;
	}
	
}
