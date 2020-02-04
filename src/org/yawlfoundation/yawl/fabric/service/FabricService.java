package org.yawlfoundation.yawl.fabric.service;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.jdom2.Element;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.fabric.bridge.HFInterface;
import org.yawlfoundation.yawl.fabric.event.BlockUpdateListener;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Michael Adams
 * @date 21/9/18
 */
public class FabricService extends AbstractService implements BlockUpdateListener {

    private static final HFInterface CLIENT = new HFInterface();

    public FabricService() {
        super();
        CLIENT.registerBlockUpdateListener(this);
    }


    @Override
    public void blockUpdateReceived(Map<String, String> funcMap) {
        for (String funcName : funcMap.values()) {
            System.out.println("UPDATE RECEIVED: " + funcName);
        }
    }


    @Override
    protected Element processWorkItem(WorkItemRecord wir) {
        String result;
        String fcn = getDataValue(wir.getDataList(), "fcn");
        if (fcn != null) {
            String args = getDataValue(wir.getDataList(), "args");
            String[] argsArray = args != null ? args.split(",") : null;
            try {
                if (fcn.startsWith("query")) {
                    result = query(fcn, argsArray);
                }
                else {
                    result = invoke(fcn, argsArray);
                }
            }
            catch (Exception e) {
                result = failMsg(e.getMessage());
            }
        }
        else {
            result = failMsg("No function name provided");
        }
        return formatOutputData(wir.getTaskID(), "result", result);
    }


    private String query(String fcn, String[] argsArray) throws Exception {
        return parseResponses(CLIENT.query(fcn, argsArray));
    }


    private String invoke(String fcn, String[] argsArray) throws Exception {
        return getInvokeResponse(CLIENT.invoke(fcn, argsArray).get(60, TimeUnit.SECONDS));
    }


    private String parseResponses(Collection<ProposalResponse> responses) throws Exception {
        StringBuilder result = new StringBuilder();
        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
                result.append(new String(response.getChaincodeActionResponsePayload()));
            }
            else {
                result.append("Response failed. status: ");
                result.append(response.getMessage());
            }
            result.append('\n');
        }
        return result.toString();
    }


    private String getInvokeResponse(BlockEvent.TransactionEvent event) {
        String result = "Transaction tx [" + event.getTransactionID() + "]";
        if (event.isValid()) {
            result += " has completed successfully.";
        } else {
            result += " was invalid.";
        }
        return result;
    }

}
