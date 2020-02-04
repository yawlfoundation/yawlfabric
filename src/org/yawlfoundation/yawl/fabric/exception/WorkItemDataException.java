package org.yawlfoundation.yawl.fabric.exception;

/**
 * An exception that may be thrown to indicate a problem with workitem data
 *
 * @author Michael Adams
 * @date 21/9/18
 * */
public class WorkItemDataException extends Exception {

    public WorkItemDataException() { super(); }

    public WorkItemDataException(String msg) { super(msg); }
}
