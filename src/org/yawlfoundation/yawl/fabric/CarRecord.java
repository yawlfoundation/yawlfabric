package org.yawlfoundation.yawl.fabric;

import javax.json.JsonObject;

/**
 * @author Michael Adams
 * @date 24/9/18
 */
public class CarRecord {

    String key;
    JsonObject record;

    public CarRecord(JsonObject rec) {
        key = rec.getString("Key");
        record = rec.getJsonObject("Record");
    }


    public String getKey() { return key; }

    public JsonObject getRecord() { return record; }
}
