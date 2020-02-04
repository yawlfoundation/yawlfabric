package org.yawlfoundation.yawl.fabric.service;

import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.yawlfoundation.yawl.fabric.bridge.HFInterface;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Michael Adams
 * @date 27/9/18
 */
public class FabricServiceServlet extends HttpServlet {

    private static final HFInterface CLIENT = new HFInterface();

//    public void init() {
//        super.init();
//    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        doPost(req, res);                                // redirect all GETs to POSTs
    }


    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        try {
            String body = IOUtils.toString(req.getReader());
            if (body == null || body.isEmpty()) {
                throw new IOException("no parameters provided");
            }
            System.out.println("#body = " + body);

            JsonObject jsonObject = readBody(body);
            String action = jsonObject.getString("action");
            String result = null;

            if (action == null || action.isEmpty()) {
                throw new IOException("no action provided");
            }
            else if (action.equals("order")) {
                result = processOrder(jsonObject.getJsonObject("items"));
            }
            else if (action.equals("query")) {
                String[] args = { jsonObject.getString("key") };
                result = query("query", args);
            }
            else if (action.equals("history")) {
                String[] args = { jsonObject.getString("key") };
                result = query("queryHistory", args);
            }
            else {
                throw new IllegalArgumentException("Invalid request");
            }

//            else if (action.equals("connect")) {
//                String userid = dis.readUTF();
//                String password = dis.readUTF();
//                result = _sessions.connect(userid, password);
//            } else if (action.equals("checkConnection")) {
//                result = String.valueOf(_sessions.checkConnection(handle));
//            } else if (action.equals("disconnect")) {
//                result = String.valueOf(_sessions.disconnect(handle));
//            } else if (_sessions.checkConnection(handle)) {
//            } else writeString(res, "Invalid or disconnected session handle", "failure");

            if (result != null) writeString(res, result);
        }
        catch (EOFException eofe) {              // occurs when the inputStream is null
            writeString(res, jsonError("No input"));
        }
        catch (Exception e) {
            writeString(res, jsonError(e.getMessage()));
        }
    }


    private String jsonError(String msg) {
        return "{ \"error\": \"" + msg + "\" }";
    }


    private void writeString(HttpServletResponse res, String msg)
            throws IOException {
        if (msg != null) {
            res.setContentType("text/json; charset=UTF-8");
            OutputStreamWriter out = new OutputStreamWriter(res.getOutputStream(), StandardCharsets.UTF_8);
            out.write(msg);
            out.flush();
            out.close();
        }
    }


    private String query(String verb, String[] args) throws InvalidArgumentException, ProposalException {
        Collection<ProposalResponse> responses = CLIENT.query(verb, args);
        if (responses.size() != 1) {
            throw new IllegalArgumentException("Invalid arguments");
        }

        ProposalResponse response = responses.iterator().next();
        if (response.isVerified() && response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
            return response.getMessage();
        }

        throw new ProposalException("Invalid response");
    }


    private JsonObject readBody(String body) {
        System.out.println("#readBody, body=" + body);
        JsonReader reader = Json.createReader(new StringReader(body));
        System.out.println("#reader created");
        JsonObject jsonObject = reader.readObject();
        System.out.println("#object created");
        reader.close();
        return jsonObject;
    }


    private String processOrder (JsonObject itemsObject)
            throws ProposalException, InvalidArgumentException, InterruptedException,
                   ExecutionException, TimeoutException {

        StringBuilder result = new StringBuilder("{");
        for (int i=0; i < itemsObject.size(); i++) {
            JsonObject itemArray = itemsObject.getValue("/" + i).asJsonObject();
            String item = itemArray.getString("item");
            String quantity = itemArray.getString("quantity");
            String total = itemArray.getString("total");

            String bundle = getProductString(item);
            String[] args = {bundle, quantity, total};
            BlockEvent.TransactionEvent event = CLIENT.invoke("invoke", args)
                    .get(60, TimeUnit.SECONDS);
            
            result.append("[ \"transaction\": \"").append(event.getTransactionID())
                    .append("\", \"result\": ")
                    .append(event.isValid() ? " \"success\"]" : " \"invalid\"]");
        }
        return result + "}";
    }

    
    private String getProductString(String itemID) {
        switch (itemID) {
            case "158" : return "edits";
            case "163" : return "report";
            case "165" : return "illustrations";
            case "173" : return "hardcopy";
        }
        return "invalid";
    }
    
}
