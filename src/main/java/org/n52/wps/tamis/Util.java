package org.n52.wps.tamis;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;


public class Util {

    public static InputStream connectWithBasicAuth(
            String uri, String userName, String password) throws HttpException, IOException{
        
        HttpClient backend = new HttpClient();           
        
        backend.getState().setCredentials(
                                 new AuthScope("fluggs.wupperverband.de", 80, null),
                             new UsernamePasswordCredentials(userName, password)
                           );
        
        GetMethod httpget = new GetMethod(uri);
        
        httpget.setDoAuthentication(true);
        
        backend.executeMethod(httpget);
        
        return httpget.getResponseBodyAsStream();
    }
    
}
