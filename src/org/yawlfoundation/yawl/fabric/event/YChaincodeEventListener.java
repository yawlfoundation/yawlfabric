package org.yawlfoundation.yawl.fabric.event;

import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Michael Adams
 * @date 2018-11-27
 */
public class YChaincodeEventListener implements ChaincodeEventListener {

    private final List<ChaincodeEventNode> _receivedEvents;
    private String _eventHandle;

    public YChaincodeEventListener(Channel channel, String expectedEventName) {
        _receivedEvents = new ArrayList<>();
        try {
            _eventHandle = channel.registerChaincodeEventListener(Pattern.compile(".*"),
                    Pattern.compile(Pattern.quote(expectedEventName)), this);
        }
        catch (InvalidArgumentException iae) {
            _eventHandle = null;
            iae.printStackTrace();
        }
    }

    @Override
    public void received(String handle, BlockEvent blockEvent, ChaincodeEvent chaincodeEvent) {
        _receivedEvents.add(new ChaincodeEventNode(handle, blockEvent, chaincodeEvent));

        Peer eventHub = blockEvent.getPeer();

        // Here put what you want to do when receive chaincode event
        System.out.println("RECEIVED CHAINCODE EVENT with handle: " + handle +
                ", chaincodeId: " + chaincodeEvent.getChaincodeId() +
                ", chaincode event name: " + chaincodeEvent.getEventName() +
                ", transactionId: " + chaincodeEvent.getTxId() +
                ", event Payload: " + new String(chaincodeEvent.getPayload()) +
                ", from eventHub: " + (eventHub != null ? eventHub.getName() : "N/A"));
    }

    
    public List<ChaincodeEventNode> getEvents() { return _receivedEvents; }


    public  String getEventHandle() { return _eventHandle; }


    public boolean waitForChaincodeEvent(int timeout, Channel channel)
        throws InvalidArgumentException {
       boolean eventDone = false;
//        if (_eventHandle != null) {
//            int numberEventsExpected = channel.getEventHubs().size() + channel
//                .getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).size();
//            log.info("numberEventsExpected: " + numberEventsExpected);
//            //just make sure we get the notifications
//            if (timeout == 0) {
//                // get event without timer
//                while (_receivedEvents.size() != numberEventsExpected) {
//                    // do nothing
//                }
//                eventDone = true;
//            } else {
//                // get event with timer
//                for (int i = 0; i < timeout; i++) {
//                    if (_receivedEvents.size() == numberEventsExpected) {
//                        eventDone = true;
//                        break;
//                    } else {
//                        try {
//                            double j = i;
//                            j = j / 10;
//                            log.info(j + " second");
//                            Thread.sleep(100); // wait for the events for one tenth of second.
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//
//            log.info("chaincodeEvents.size(): " + _receivedEvents.size());
//
//            // unregister event listener
//            channel.unregisterChaincodeEventListener(chaincodeEventListenerHandle);
//            int i = 1;
//            // arrived event handling
//            for (ChaincodeEventNode chaincodeEventCapture : _receivedEvents) {
//                log.info("Event number. " + i);
//                log.info("event capture object: " + chaincodeEventCapture.toString());
//                log.info("Event Handle: " + chaincodeEventCapture.getHandle());
//                log.info("Event TxId: " + chaincodeEventCapture.getChaincodeEvent().getTxId());
//                log.info("Event Name: " + chaincodeEventCapture.getChaincodeEvent().getEventName());
//                log.info("Event Payload: " + chaincodeEventCapture.getChaincodeEvent()
//                    .getPayload()); // byte
//                log.info("Event ChaincodeId: " + chaincodeEventCapture.getChaincodeEvent()
//                    .getChaincodeId());
//                BlockEvent blockEvent = chaincodeEventCapture.getBlockEvent();
//                try {
//                    log.info("Event Channel: " + blockEvent.getChannelId());
//                } catch (InvalidProtocolBufferException e) {
//                    e.printStackTrace();
//                }
//                log.info("Event Hub: " + blockEvent.getEventHub());
//
//                i++;
//            }
//
//        } else {
//            log.info("chaincodeEvents.isEmpty(): " + _receivedEvents.isEmpty());
//        }
//        log.info("eventDone: " + eventDone);
        return eventDone;
    }

}

