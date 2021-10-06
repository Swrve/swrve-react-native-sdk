package com.swrve.reactnative;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.swrve.sdk.SwrvePushNotificationListener;
import com.swrve.sdk.SwrveSilentPushListener;
import com.swrve.sdk.SwrveResourcesListener;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.swrve.reactnative.SwrvePluginModule.LOG_TAG;

@SuppressLint("LogNotTimber")
public class SwrvePluginEventEmitter extends ReactContextBaseJavaModule
        implements SwrvePushNotificationListener, SwrveSilentPushListener, SwrveResourcesListener {

    private final String PUSH_EVENT_NAME = "PushNotification";
    private final String SILENT_PUSH_EVENT_NAME = "SilentPushNotification";
    private final String PUSH_EVENT_PAYLOAD = "PushEventPayload";
    private final String CALLBACK_KEY_CUSTOM_BUTTON_ACTION = "customAction";
    private final String CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT = "campaignSubject";
    private final String CALLBACK_KEY_DISMISS_BUTTON_NAME = "buttonName";
    private final String CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT = "clipboardContents";
    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP = "embeddedMessagePersonalizationProperties";
    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_MAP = "embeddedMessage";

    private final String RESOURCES_UPDATED_EVENT_NAME = "SwrveUserResourcesUpdated";
    private final String MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = "SwrveMessageCustomCallback";
    private final String MESSAGE_CALLBACK_DISMISS_EVENT_NAME = "SwrveMessageDismissCallback";
    private final String MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = "SwrveMessageClipboardCallback";
    private final String MESSAGE_CALLBACK_PERSONALISATION_EVENT_NAME = "SwrveMessagePersonalisationCallback";
    private final String EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME = "SwrveEmbeddedMessageCallback";
    private final String MODULE_NAME = "SwrvePluginEventEmitter";

    private boolean listeningCustom = false;

    private final ReactApplicationContext reactContext;

    SwrvePluginEventEmitter(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    /** @see SwrvePushNotificationListener */
    @Override
    public void onPushNotification(JSONObject payload) {
        Log.i(LOG_TAG, "onPushNotification" + payload.toString());

        try {
            WritableMap params = Arguments.createMap();
            params.putString(PUSH_EVENT_PAYLOAD, payload.toString(4));
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(PUSH_EVENT_NAME,
                    params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit push notification", e);
        }
    }

    /** @see SwrveSilentPushListener */
    @Override
    public void onSilentPush(Context context, JSONObject payload) {
        Log.i(LOG_TAG, "onSilentPush" + payload.toString());

        try {
            WritableMap params = Arguments.createMap();
            params.putString(PUSH_EVENT_PAYLOAD, payload.toString(4));
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(SILENT_PUSH_EVENT_NAME,
                    params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit silent push notification", e);
        }
    }

    /** @see SwrveResourcesListener */
    @Override
    public void onResourcesUpdated() {
        try {
            WritableMap params = Arguments.createMap();
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(RESOURCES_UPDATED_EVENT_NAME, params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit user resources updated notification", e);
        }
    }

    public void onCustomAction(String customAction) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_CUSTOM_BUTTON_ACTION, customAction);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(MESSAGE_CALLBACK_CUSTOM_EVENT_NAME, params);

            if (this.listeningCustom == false) {
                processAction(customAction);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit custom button callback notification", e);
        }
    }

    public void onClipboardAction(String clipboardContents) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT, clipboardContents);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME, params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit clipboard button callback notification", e);
        }
    }

    public void onDismissAction(String campaignSubject, String buttonName) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT, campaignSubject);
            params.putString(CALLBACK_KEY_DISMISS_BUTTON_NAME, buttonName);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(MESSAGE_CALLBACK_DISMISS_EVENT_NAME, params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit dismiss button callback notification", e);
        }
    }

    public void onEmbeddedMessageCallback(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties) {
        try {
            WritableMap params = Arguments.createMap();

            JSONObject embeddedMessageJSON = embeddedMessageToJson(message);
            params.putMap(CALLBACK_KEY_EMBEDDED_MESSAGE_MAP, SwrvePluginUtils.convertJsonToMap(embeddedMessageJSON));

            if(personalizationProperties != null) {
                WritableMap personalizationMap = SwrvePluginUtils.convertJsonToMap(new JSONObject(personalizationProperties));
                params.putMap(CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP, personalizationMap);
            }
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME, params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit embedded message triggered event", e);
        }
    }

    public void processAction(String unprocessedAction) {
        try {
            reactContext.getCurrentActivity()
                    .startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(unprocessedAction)));
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't launch action as url: %s", e);
        }
    }

    public void setListeningCustom(boolean listeningCustom) {
        this.listeningCustom = listeningCustom;
    }

    private JSONObject embeddedMessageToJson(SwrveEmbeddedMessage message) throws JSONException {
        JSONObject embeddedMessageJSON = new JSONObject();

        embeddedMessageJSON.put("campaignId", message.getCampaign().getId());
        embeddedMessageJSON.put("messageId", message.getId());
        embeddedMessageJSON.put("priority", message.getPriority());
        embeddedMessageJSON.put("type", message.getType());
        embeddedMessageJSON.put("data", message.getData());

        JSONArray buttons = new JSONArray();

        for(String button : message.getButtons()) {
            buttons.put(button);
        }
        embeddedMessageJSON.put("buttons", buttons);

        return embeddedMessageJSON;
    }
}
