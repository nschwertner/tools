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

import java.io.*;
import java.util.UUID;

public class DataLoad {
    private static final String STATIC_DATA_PATH = "org/hspconsortium/platform/sample/clinicaldata/dstu2";
    private static String RESOURCE_URI = "https://hspc.isalusconsulting.com/dstu2-open-hsp-api/data";
//    private static String RESOURCE_URI = "http://localhost:8080/hsp-rest-api-webapp/data";
    private static String OUTPUT_DIR = "output";
    private static boolean isXML = true;

    public static void main(String[] args) throws Exception{

        boolean userProvidedDir = false;
        boolean writeOutput = true;
        boolean canWrite = false;

        for (int i = 0; i< args.length; i++) {

            switch (args[i]){
                case "-h" :
                    System.out.println("java -jar hsp-tools.jar org.hspconsortium.DataLoad [options]");
                    System.out.println("   Options:");
                    System.out.println("   -h       print this message");
                    System.out.println("   -url     the url for the hsp api ex: -url http://localhost:8080/hsp-api");
                    System.out.println("   -json    indicates that the input files are JSON, XML is the default");
                    System.out.println("   -out     the output directory for results; default '<current dir>/output'");
                    System.out.println("            unless the current directory is 'target', the output directory will");
                    System.out.println("            be created one directory up from 'target'");
                    System.out.println("            NOTE: the import will fail if results can't be written out");
                    System.out.println("   -no-out  no output for results");
                    return;
                case "-url" :
                    RESOURCE_URI = args[++i];
                    break;
                case "-out" :
                    userProvidedDir = true;
                    OUTPUT_DIR = args[++i];
                    break;
                case "-no-out" :
                    writeOutput = false;
                    break;
                case "-json" :
                    isXML = false;
                    break;
            }
        }

        //Test writing output
        if (userProvidedDir || writeOutput) {
            canWrite = checkOutput(OUTPUT_DIR, userProvidedDir);
            System.out.println("can write: " + canWrite);
            if (!canWrite ) {
                return; //Exit if results can't be written out
            }
        }

        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource[] patientFileResources = resourceResolver.getResources(String.format("classpath:%s/*", STATIC_DATA_PATH));

        for(org.springframework.core.io.Resource patientFile : patientFileResources){
            if (!patientFile.isReadable()) {
                continue;
            }
            byte[] xmlString = IOUtils.toByteArray(patientFile.getInputStream());

            HttpResponse response = post(xmlString);
            System.out.println("File: " + patientFile.toString());
            StatusLine sl = response.getStatusLine();
            System.out.println("Status: " + sl.getStatusCode());
            if (sl.getStatusCode() != 200) {
                System.out.println("Cause: " + sl.toString());
            } else if (canWrite) {
                writeOutput(response);
            }
        }
    }

    private static void writeOutput(HttpResponse response) {
        OutputStream outputStream = null;

        try {
            String outputDir = OUTPUT_DIR + "/" + getUinqueID() + ".json";
            System.out.println("Output: " + outputDir);
            outputStream = new FileOutputStream(outputDir);
            IOUtils.copy(response.getEntity().getContent(), outputStream);
        } catch (IOException ex) {
            // report
        } finally {
            try {outputStream.close();} catch (Exception ex) {}
        }
    }

    private static boolean checkOutput(String directoryName, boolean userProvidedDir) {
        boolean canWriteFiles = false;
        String workingDir = directoryName;
        if (!userProvidedDir) {
            workingDir = System.getProperty("user.dir");
            if (workingDir.endsWith("/target")) {
                workingDir = workingDir.substring(0, workingDir.lastIndexOf("/target")+1);
            }

            if (directoryName.equalsIgnoreCase("output")) {
                workingDir = workingDir + directoryName;
            }
        }
        File theDir = new File(workingDir);
        System.out.println("Output Directory: " + workingDir);


        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("creating directory: " + workingDir);

            try{
                theDir.mkdir();
                if (theDir.exists()) {
                    canWriteFiles = true;
                }
            } catch(SecurityException se){
                return false;
            }
            if(canWriteFiles) {
                System.out.println("DIR created");
            }
        } else {
            File testFile  = new File(workingDir + "/testfile");
            try {
                testFile.createNewFile();
                if (testFile.exists()) {
                    canWriteFiles = true;
                }
                testFile.deleteOnExit();
            } catch (IOException e) {
                return false;
            }
        }
        OUTPUT_DIR = workingDir;
        return canWriteFiles;
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
        if (isXML){
            request.addHeader("Accept", "application/xml+fhir");
            request.addHeader("Content-Type", "application/xml+fhir;charset=" + "UTF-8");
        } else {
            request.addHeader("Accept", "application/json+fhir");
            request.addHeader("Content-Type", "application/json+fhir;charset=" + "UTF-8");
        }
        request.addHeader("Accept-Charset", "UTF-8");
    }

    private static String getUinqueID(){
        return UUID.randomUUID().toString();
    }

}
