package org.hspconsortium;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

public class DataLoad {
    private static final String STATIC_DATA_PATH = "org/hspconsortium/platform/sample/clinicaldata";
    private static String RESOURCE_URI = "http://localhost:8080/hsp-api";

    public static void main(String[] args) throws Exception{

        for (int i = 0; i< args.length; i++) {

            switch (args[i]){
                case "-h" :
                    System.out.println("java -jar hsp-tools.jar org.hspconsortium.DataLoad [options]");
                    System.out.println("   Options:");
                    System.out.println("   -h    print this message");
                    System.out.println("   -url  the url for the hsp api ex: -url http://localhost:8080/hsp-api");
                    return;
                case "-url" :
                    RESOURCE_URI = args[++i];
                    break;
            }
        }

        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[] patientFileResources = resourceResolver.getResources(String.format("classpath:%s/*.fhir-bundle.xml", STATIC_DATA_PATH));

        for(org.springframework.core.io.Resource patientFile : patientFileResources){
            byte[] xmlString = IOUtils.toByteArray(patientFile.getInputStream());

            HttpResponse response = post(xmlString);
            System.out.println("File: " + patientFile.toString());
            StatusLine sl = response.getStatusLine();
            System.out.println("Status: " + sl.getStatusCode());
            if (sl.getStatusCode() != 200) {
                System.out.println("Cause: " + sl.toString());
            }
        }
    }

    protected static HttpResponse post(byte[] payload){
        HttpPost postRequest = new HttpPost(RESOURCE_URI);
        setRequestHeaders(postRequest);
        return sendPayload(postRequest, payload);
    }

    protected static HttpResponse sendPayload(HttpEntityEnclosingRequestBase request, byte[] payload) {
        HttpResponse response = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            request.setEntity(new ByteArrayEntity(payload));
            response = httpclient.execute(request);
        } catch(IOException ioe) {
            throw new RuntimeException("Error sending HTTP Post/Put Payload", ioe);
        }
        return response;
    }

    protected static void setRequestHeaders(HttpRequest request){
        request.addHeader("User-Agent", "Java FHIR Client for FHIR");
        request.addHeader("Accept", "application/fhir+json");
        request.addHeader("Content-Type", "application/xml;charset=" + "UTF-8");
        request.addHeader("Accept-Charset", "UTF-8");
    }
}
