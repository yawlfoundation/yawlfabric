package org.yawlfoundation.yawl.fabric.event;

import java.util.Map;

/**
 * @author Michael Adams
 * @date 2018-11-28
 */
public interface BlockUpdateListener {

    void blockUpdateReceived(Map<String, String> funcMap);
}
