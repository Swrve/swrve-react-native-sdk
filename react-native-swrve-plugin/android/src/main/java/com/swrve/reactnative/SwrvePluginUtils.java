package com.swrve.reactnative;

import android.annotation.SuppressLint;
import android.util.Log;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import static com.swrve.reactnative.SwrvePluginModule.LOG_TAG;

@SuppressLint("LogNotTimber")
public class SwrvePluginUtils {

  static Map<String, String> convertToStringMap(ReadableMap readableMap) {
    Map<String, String> map = new HashMap<>();
    ReadableMapKeySetIterator iterator = readableMap.keySetIterator();

    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      ReadableType type = readableMap.getType(key);

      switch (type) {
      case Boolean:
        map.put(key, String.valueOf(readableMap.getBoolean(key)));
        break;
      case Number:
        map.put(key, String.valueOf(readableMap.getDouble(key)));
        break;
      case String:
        map.put(key, String.valueOf(readableMap.getString(key)));
        break;
      case Map:
      case Array:
      case Null:
        break;
      }
    }

    return map;
  }

  static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {

    try {
      WritableMap map = new WritableNativeMap();

      Iterator<String> iterator = jsonObject.keys();
      while (iterator.hasNext()) {
        String key = iterator.next();
        Object value = jsonObject.get(key);
        if (value instanceof JSONObject) {
          map.putMap(key, convertJsonToMap((JSONObject) value));
        } else if (value instanceof JSONArray) {
          map.putArray(key, convertJsonToArray((JSONArray) value));
        } else if (value instanceof Boolean) {
          map.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
          map.putInt(key, (Integer) value);
        } else if (value instanceof Double) {
          map.putDouble(key, (Double) value);
        } else if (value instanceof String) {
          map.putString(key, (String) value);
        } else {
          map.putString(key, value.toString());
        }
      }
      return map;
    } catch (final UnsatisfiedLinkError e) {
      Log.d(LOG_TAG, "Create WritableNativeMap " + Log.getStackTraceString(e));
    }
    return null;
  }

  static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
    try {
      WritableArray array = new WritableNativeArray();

      for (int i = 0; i < jsonArray.length(); i++) {
        Object value = jsonArray.get(i);
        if (value instanceof JSONObject) {
          array.pushMap(convertJsonToMap((JSONObject) value));
        } else if (value instanceof JSONArray) {
          array.pushArray(convertJsonToArray((JSONArray) value));
        } else if (value instanceof Boolean) {
          array.pushBoolean((Boolean) value);
        } else if (value instanceof Integer) {
          array.pushInt((Integer) value);
        } else if (value instanceof Double) {
          array.pushDouble((Double) value);
        } else if (value instanceof String) {
          array.pushString((String) value);
        } else {
          array.pushString(value.toString());
        }
      }
      return array;
    } catch (final UnsatisfiedLinkError e) {
      Log.d(LOG_TAG, "Create WritableNativeArray " + Log.getStackTraceString(e));
    }
    return null;
  }

  static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
    JSONObject object = new JSONObject();
    ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      switch (readableMap.getType(key)) {
      case String:
        object.put(key, readableMap.getString(key));
        break;
      case Number:
        object.put(key, readableMap.getInt(key));
        break;
      case Boolean:
        object.put(key, readableMap.getBoolean(key));
        break;
      case Map:
        object.put(key, convertMapToJson(readableMap.getMap(key)));
        break;
      case Array:
        object.put(key, convertArrayToJson(readableMap.getArray(key)));
        break;
      case Null:
        object.put(key, JSONObject.NULL);
        break;
      }
    }
    return object;
  }

  private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
    JSONArray array = new JSONArray();
    for (int i = 0; i < readableArray.size(); i++) {
      switch (readableArray.getType(i)) {
      case Boolean:
        array.put(readableArray.getBoolean(i));
        break;
      case Number:
        array.put(readableArray.getInt(i));
        break;
      case String:
        array.put(readableArray.getString(i));
        break;
      case Map:
        array.put(convertMapToJson(readableArray.getMap(i)));
        break;
      case Array:
        array.put(convertArrayToJson(readableArray.getArray(i)));
        break;
      case Null:
        break;
      }
    }
    return array;
  }

}
