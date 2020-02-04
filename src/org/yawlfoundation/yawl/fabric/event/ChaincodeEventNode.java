package org.yawlfoundation.yawl.fabric.event;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEvent;

/**
 * @author Michael Adams
 * @date 2018-11-27
 */
public class ChaincodeEventNode {
    private final String _handle;
    private final BlockEvent _blockEvent;
    private final ChaincodeEvent _chaincodeEvent;


    public ChaincodeEventNode(String handle, BlockEvent blockEvent,
                              ChaincodeEvent chaincodeEvent) {
        _handle = handle;
        _blockEvent = blockEvent;
        _chaincodeEvent = chaincodeEvent;
    }


    public String getHandle() { return _handle; }

    public BlockEvent getBlockEvent() { return _blockEvent; }

    public ChaincodeEvent getChaincodeEvent() { return _chaincodeEvent; }
}
