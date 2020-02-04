package org.yawlfoundation.yawl.fabric.event;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo;
import org.hyperledger.fabric.sdk.BlockListener;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Michael Adams
 * @date 2018-11-28
 */
public class YBlockListener implements BlockListener {

    private final Set<BlockUpdateListener> _updateListeners = new HashSet<>();

    @Override
    public void received(BlockEvent blockEvent) {
        try {
            Map<String, String> transactionMap = new HashMap<>();
            for (TransactionEvent transactionEvent : blockEvent.getTransactionEvents()) {
                if (! transactionEvent.isValid()) continue;
                for (TransactionActionInfo tai : transactionEvent.getTransactionActionInfos()) {
                    if (tai.getChaincodeInputArgsCount() > 0) {
                        byte[] argbytes = tai.getChaincodeInputArgs(0);
                        String funcName = new String(argbytes, StandardCharsets.UTF_8);
                        transactionMap.put(transactionEvent.getTransactionID(), funcName);
                    }
                }
            }
            announce(transactionMap);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void registerUpdateListener(BlockUpdateListener listener) {
        _updateListeners.add(listener);
    }
    

    public boolean unregisterUpdateListener(BlockUpdateListener listener) {
        return _updateListeners.remove(listener);
    }


    private void announce(Map<String, String> transactionMap) {
        if (! transactionMap.isEmpty()) {
            for (BlockUpdateListener listener : _updateListeners) {
                listener.blockUpdateReceived(transactionMap);
            }
        }
    }
}
