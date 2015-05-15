package com.segment.analytics.internal.integrations;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by VicV on 5/14/15.
 * <p/>
 * This is a JSONObject used specifically for testing. It overrides the .equals() method
 * and does a proper comparison of the two objects' contents to determine if they are
 * truly equal.
 * <p/>
 * This was created because Mockito's eq matcher does not properly compare JSONObjects,
 * Also, it doesn't seem to me that segment has JSONAssert in their testing stuff.
 */
public class TaplyticsJSONObject extends JSONObject {

    @Override
    public boolean equals(Object o) {
        try {
            if (o instanceof JSONObject) {
                return JSONEqual((JSONObject) o, this);
            }
        } catch (JSONException e) {
            return false;
        }
        return false;
    }


    public boolean JSONEqual(JSONObject incoming, JSONObject original) throws JSONException {
        //If either is null, check if the other is too.
        if (incoming == null || original == null) {
            return (incoming == original);
        }
        //ArrayList of all the keys for the incoming JSONObject.
        ArrayList<String> incomingKeys = new ArrayList<>();

        //Grab the key set from the incoming JSONObject
        JSONArray incomingKeysJSONArray = incoming.names();

        //Put all the keys into the ArrayList
        if (incomingKeysJSONArray != null) {
            for (int i = 0; i < incomingKeysJSONArray.length(); i++) {
                incomingKeys.add(incomingKeysJSONArray.get(i).toString());
            }
        }
        //Sort the key set.
        Collections.sort(incomingKeys);

        //Repeat the above for the original JSONObject's keys.
        ArrayList<String> originalKeys = new ArrayList<>();
        JSONArray originalKeysJSONArray = original.names();
        if (originalKeysJSONArray != null) {
            for (int i = 0; i < originalKeysJSONArray.length(); i++) {
                originalKeys.add(originalKeysJSONArray.get(i).toString());
            }
        }
        Collections.sort(originalKeys);

        //If the keys aren't the same then the objects obviously aren't.
        if (!incomingKeys.equals(originalKeys)) {
            return false;
        }

        //Iterate through the incoming object's keys now to make sure the values are equal.
        for (String key : incomingKeys) {

            //Get the two values for the given key.
            Object incomingValue = incoming.get(key);
            Object originalValue = original.get(key);

            //Deal with nested JSONObjects.
            if (incomingValue instanceof JSONObject) {
                if (!(originalValue instanceof JSONObject)) {
                    return false;
                }
                if (!JSONEqual((JSONObject) incomingValue, (JSONObject) originalValue)) {
                    return false;
                } else {
                    continue;
                }
            }

            //If the value is null for the incoming JSONObject and the original is not, not equal!
            if (incomingValue == null) {
                if (originalValue != null) {
                    return false;
                }

                //Finally the simple check of whether or not the values are equal.
            } else if (!incomingValue.equals(originalValue)) {
                return false;
            }
        }
        return true;
    }
}

