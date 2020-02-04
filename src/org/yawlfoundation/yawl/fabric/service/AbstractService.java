package org.yawlfoundation.yawl.fabric.service;

import org.jdom2.Element;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceBWebsideController;
import org.yawlfoundation.yawl.fabric.exception.WorkItemDataException;
import org.yawlfoundation.yawl.util.StringUtil;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Abstracts common functionality required by YAWL custom services
 *
 * @author Michael Adams
 * @date 16/09/18
 */
public abstract class AbstractService extends InterfaceBWebsideController {

    // for inserting empty values into output data Elements
    protected static final String EMPTY_STRING = "";

    protected static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    protected String _handle = null;      // stores a session handle to the YAWL engine


    public AbstractService() {
        super();
    }

    /**
     * Receives a notification from the YAWL Engine of the enabling of a work item
     * to be delegated to this service. Performs the operations required to start,
     * process and complete the work item.
     *
     * @param wir the enabled work item
     */
    public void handleEnabledWorkItemEvent(WorkItemRecord wir) {
        try {

            // connect only if not already connected
            if (!connected()) connect();

            // checkout ... process ... checkin
            wir = checkOut(wir.getID(), _handle);
            Element output = processWorkItem(wir);
            if (output == null) {
                throw new IllegalArgumentException("Output data element is null");
            }
            checkInWorkItem(wir.getID(), wir.getDataList(), output, null, _handle);
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    // have to implement abstract method, but have no need for this event
    public void handleCancelledWorkItemEvent(WorkItemRecord wir) {
    }


    /**
     * Checks whether the service has a currently active connection with the YAWL Engine
     *
     * @return true if the service is connected to the Engine, false if not
     * @throws IOException if the YAWL Engine cannot be reached
     */
    protected boolean connected() throws IOException {
        return _handle != null && checkConnection(_handle);
    }


    /**
     * Sets up a connection to the YAWL engine. The credentials used are read from
     * the service's web.xml. If successful, a session handle is created and stored
     * for all service <-> engine communication.
     *
     * @throws IOException if there's a problem connecting to the engine
     */
    protected void connect() throws IOException {
        String result = connect(engineLogonName, engineLogonPassword);
        if (!successful(result)) {
            _handle = null;
            throw new IOException(StringUtil.unwrap(result));
        }
        _handle = result;
    }


    /**
     * Extracts a work item's input data Element
     *
     * @param wir the work item being handled
     * @return a JDOM Element containing the work item's input data
     * @throws WorkItemDataException if the work item is null or doesn't contain
     *                               any input data
     */
    protected Element getInputData(WorkItemRecord wir) throws WorkItemDataException {
        if (wir == null) {
            throw new WorkItemDataException("Work item is null.");
        }
        Element data = wir.getDataList();
        if (data == null) {
            throw new WorkItemDataException("Work item contains no input data.");
        }
        return data;
    }


    /**
     * Gets a data variable's value from a work item's input data
     *
     * @param data    the work item's input data
     * @param varName the name of the variable to get the value of
     * @return the variable's value
     * @throws WorkItemDataException if the work item's input data doesn't contain
     *                               a variable with the name provided, or the value retrieved is null
     */
    protected String getRequiredValue(Element data, String varName) throws WorkItemDataException {
        String value = getDataValue(data, varName);
        if (value == null) {
            throw new WorkItemDataException(
                    "Work item does not provide a required input value for parameter: " + varName);
        }
        return value;
    }


    /**
     * Builds a JDOM Element of the structure required to represent the output data of
     * a work item
     *
     * @param taskName the name of the task the work item is an instance of
     * @param varName  the name of the data variable
     * @param value    the value to assign to the data variable
     * @return the correctly structured output Element
     */
    protected Element formatOutputData(String taskName, String varName, String value) {
        Element output = new Element(taskName);   // outer element must have name of task
        Element result = new Element(varName);
        result.setText(value);
        output.addContent(result);
        return output;
    }


    /**
     * Builds a JDOM Element of the structure required to represent the output data of
     * a work item. Vars are added in the order listed.
     *
     * @param taskName the name of the task the work item is an instance of
     * @param dataMap  a sorted map of [data item name, data item value]. Variables are
     *                 added to the output Element in the order of their position in the
     *                 map
     * @return the correctly structured output Element
     */
    protected Element formatOutputData(String taskName,
                                       LinkedHashMap<String, String> dataMap) {
        Element output = new Element(taskName);   // outer element must have name of task
        for (String name : dataMap.keySet()) {
            Element result = new Element(name);
            result.setText(dataMap.get(name));
            output.addContent(result);
        }
        return output;
    }


    /**
     * Extracts the value assigned to a data variable within a JDOM Element
     *
     * @param data    the Element containing the variable
     * @param varName the name of the data variable
     * @return the value assigned to the data variable, or null if either the data
     * element is null, or the element doesn't contain the named variable
     */
    protected String getDataValue(Element data, String varName) {
        return (data != null) ? data.getChildText(varName) : null;
    }


    /**
     * Creates a new YParameter
     *
     * @param IorO     the parameter's scope - one of YParameter._INPUT_PARAM_TYPE or
     *                 YParameter._OUTPUT_PARAM_TYPE
     * @param type     the data type
     * @param name     the parameter name
     * @param doco     a description of the parameter
     * @param optional whether the variable requires a value at runtime
     * @return the new object
     */
    protected YParameter createParameter(int IorO, String type, String name,
                                         String doco, boolean optional) {
        YParameter param = new YParameter(null, IorO);
        param.setDataTypeAndName(type, name, XSD_NAMESPACE);
        param.setDocumentation(doco);
        param.setOptional(optional);
        return param;
    }


    /**
     * Creates a new YParameter of Input scope
     *
     * @param type     the data type
     * @param name     the parameter name
     * @param doco     a description of the parameter
     * @param optional whether the variable requires a value at runtime
     * @return the new object
     */
    protected YParameter createInputParameter(String type, String name,
                                              String doco, boolean optional) {
        return createParameter(YParameter._INPUT_PARAM_TYPE, type, name, doco, optional);
    }


    /**
     * Creates a new YParameter of Output scope
     *
     * @param type     the data type
     * @param name     the parameter name
     * @param doco     a description of the parameter
     * @param optional whether the variable requires a value at runtime
     * @return the new object
     */
    protected YParameter createOutputParameter(String type, String name,
                                               String doco, boolean optional) {
        return createParameter(YParameter._OUTPUT_PARAM_TYPE, type, name, doco, optional);
    }


    /**
     * Constructs a failure message
     *
     * @param msg the message content
     * @return a failure message of the form "<failure>msg content</failure>"
     */
    protected String failMsg(String msg) {
        if (StringUtil.isNullOrEmpty(msg)) msg = " ";   // force <failure></failure>
        return StringUtil.wrap(msg, "failure");
    }


    /**
     * An abstract method to be implemented by sub-classes to actually perform the
     * work of the work item
     *
     * @param wir the work item to process
     * @return the output data Element for the work item
     */
    protected abstract Element processWorkItem(WorkItemRecord wir);

}