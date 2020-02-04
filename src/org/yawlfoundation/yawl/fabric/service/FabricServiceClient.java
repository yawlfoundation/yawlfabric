package org.yawlfoundation.yawl.fabric.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Michael Adams
 * @date 29/10/19
 */
public class FabricServiceClient {

    private String _backendURI = "http://localhost:8080/fsGateway/";

    public FabricServiceClient() { }

    public FabricServiceClient(String backendURI) {
        _backendURI = backendURI;
    }


    public String addTransaction(String bundle, String salesAmount) throws IOException {
        byte[] bytes = toByteArray("invoke", bundle, salesAmount);
        return executePost(bytes).toString("UTF-8");
    }


    public String query(String key) throws IOException {
        byte[] bytes = toByteArray("query", key);
        return executePost(bytes).toString("UTF-8");
    }

    public String history(String key) throws IOException {
        byte[] bytes = toByteArray("history", key);
        return executePost(bytes).toString("UTF-8");
    }



    private byte[] toByteArray(String action, String handle, String... args) throws IOException {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         DataOutputStream d = new DataOutputStream(baos);
         d.writeUTF(action);
         d.writeUTF(handle);
         if (args != null) {
             for (String arg : args) {
                 d.writeUTF(arg);
             }
         }
         return baos.toByteArray();
     }


     private ByteArrayOutputStream executePost(byte[] bytes) throws IOException {
         URL url = new URL(_backendURI);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();
         connection.setDoOutput(true);
         connection.setRequestProperty("Content-Type", "multipart/form-data");
         connection.setRequestProperty("Content-length", "" + bytes.length);
         connection.setRequestProperty("Connection", "close");
         connection.getOutputStream().write(bytes);
         connection.getOutputStream().close();
         ByteArrayOutputStream outStream = getOutStream(connection.getInputStream());
         connection.disconnect();
         return outStream;
     }


     private ByteArrayOutputStream getOutStream(InputStream is) throws IOException {
         final int BUF_SIZE = 32768;

         // read reply into a buffered byte stream - to preserve UTF-8
         BufferedInputStream inStream = new BufferedInputStream(is);
         ByteArrayOutputStream outStream = new ByteArrayOutputStream(BUF_SIZE);
         byte[] buffer = new byte[BUF_SIZE];

         // read chunks from the input stream and write them out
         int bytesRead;
         while ((bytesRead = inStream.read(buffer, 0, BUF_SIZE)) > 0) {
             outStream.write(buffer, 0, bytesRead);
         }
         outStream.close();
         inStream.close();

         return outStream;
     }

}
