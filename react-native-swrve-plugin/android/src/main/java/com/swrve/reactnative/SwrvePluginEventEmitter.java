package com.swrve.reactnative;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveInAppMessageListener;
import com.swrve.sdk.messaging.SwrveMessageButtonDetails;
import com.swrve.sdk.messaging.SwrveMessageDetails;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

public class SwrvePluginEventEmitter extends ReactContextBaseJavaModule {

    private final String PUSH_EVENT_NAME = "PushNotification";
    private final String SILENT_PUSH_EVENT_NAME = "SilentPushNotification";
    private final String PUSH_EVENT_PAYLOAD = "PushEventPayload";
    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP = "embeddedMessagePersonalizationProperties";
    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_MAP = "embeddedMessage";
    private final String CALLBACK_KEY_EMBEDDED_CONTROL_ACTION = "embeddedMessageIsControl";

    // InAppListener property
    private final String CALLBACK_KEY_MESSAGE_DETAIL_MAP = "messageDetail";
    private final String CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON = "messageDetailSelectedButton";
    private final String CALLBACK_KEY_MESSAGE_DETAIL_ACTION = "messageDetailAction";

    // Deeplink listener
    private final String CALLBACK_KEY_DEEPLINK_ACTION_STRING = "deeplinkDelegateActionString";

    // Event name (this is bing used by JS layer to listen)
    private final String RESOURCES_UPDATED_EVENT_NAME = "SwrveUserResourcesUpdated";
    private final String EMBEDDED_CALLBACK_EVENT_NAME = "SwrveEmbeddedCallback";
    private final String MESSAGE_DETAILS_DELEGATE_EVENT_NAME = "SwrveMessageDetailsDelegate";
    private final String DEEPLINK_DELEGATE_EVENT_NAME = "SwrveMessageDeeplinkDelegate";
    private final String PUSHINBOX_UPDATE_EVENT_NAME = "SwrvePushInboxUpdate";

    private boolean listeningDeeplink = false;
    private boolean listeningPushInboxUpdate = false;
    private final ReactApplicationContext reactContext;

    SwrvePluginEventEmitter(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "SwrvePluginEventEmitter";
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Keep: Required for NativeEventEmitter to work correctly
    }

    @ReactMethod
    public void removeListeners(double count) {
        // Keep: Required for NativeEventEmitter to work correctly
    }

    protected void onPushNotification(JSONObject payload) {
        SwrveLogger.v("SwrvePlugin: onPushNotification:%s", payload.toString());
        try {
            WritableMap params = Arguments.createMap();
            params.putString(PUSH_EVENT_PAYLOAD, payload.toString(4));
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(PUSH_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to emit push notification", e);
        }
    }

    protected void onSilentPush(Context context, JSONObject payload) {
        SwrveLogger.v("SwrvePlugin: onSilentPush:%s", payload.toString());
        try {
            WritableMap params = Arguments.createMap();
            params.putString(PUSH_EVENT_PAYLOAD, payload.toString(4));
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(SILENT_PUSH_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to emit silent push notification", e);
        }
    }

    protected void onResourcesUpdated() {
        try {
            WritableMap params = Arguments.createMap();
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(RESOURCES_UPDATED_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to emit user resources updated notification", e);
        }
    }

    protected void onEmbeddedCallback(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties, Boolean isControl) {
        try {
            WritableMap params = Arguments.createMap();
            JSONObject embeddedMessageJSON = embeddedMessageToJson(message);
            params.putMap(CALLBACK_KEY_EMBEDDED_MESSAGE_MAP, SwrvePluginUtils.convertJsonToMap(embeddedMessageJSON));
            if (personalizationProperties != null) {
                WritableMap personalizationMap = SwrvePluginUtils.convertJsonToMap(new JSONObject(personalizationProperties));
                params.putMap(CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP, personalizationMap);
            }
            String isControlFlag = isControl ? "true" : "false";
            params.putString(CALLBACK_KEY_EMBEDDED_CONTROL_ACTION, isControlFlag);

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(EMBEDDED_CALLBACK_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to emit embedded message triggered event", e);
        }
    }

    protected void onMessageDetailDelegate(SwrveInAppMessageListener.SwrveMessageAction action, SwrveMessageDetails messageDetails, SwrveMessageButtonDetails selectedButton) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_MESSAGE_DETAIL_ACTION, action.toString());
            JSONObject json = messageDetailsToJson(messageDetails);
            params.putMap(CALLBACK_KEY_MESSAGE_DETAIL_MAP, SwrvePluginUtils.convertJsonToMap(json));
            if (selectedButton != null) {
                WritableMap buttonMap = SwrvePluginUtils.convertJsonToMap(buttonDetailsToJson(selectedButton));
                params.putMap(CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON, buttonMap);
            }
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(MESSAGE_DETAILS_DELEGATE_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Error in onMessageDetailDelegate", e);
        }
    }

    protected void onDeeplinkDelegate(String actionString) {
        SwrveLogger.v("SwrvePlugin: onDeeplinkDelegate actionString:%s listeningDeeplink:%s", actionString, listeningDeeplink);
        if (!listeningDeeplink) {
            processAction(actionString);
            return;
        }

        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_DEEPLINK_ACTION_STRING, actionString);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(DEEPLINK_DELEGATE_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to  emit swrve message details triggered event", e);
        }
    }

    protected void onPushInboxUpdate() {
        SwrveLogger.v("SwrvePlugin: onPushInboxUpdate");
        if (!listeningPushInboxUpdate) {
            return;
        }

        try {
            WritableMap params = Arguments.createMap();
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(PUSHINBOX_UPDATE_EVENT_NAME, params);
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to  emit swrve message details triggered event", e);
        }
    }

    protected void processAction(String unprocessedAction) {
        try {
            reactContext.getCurrentActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(unprocessedAction)));
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Couldn't launch action as url: %s", unprocessedAction, e);
        }
    }

    protected void setListeningDeeplink(boolean listeningDeeplink) {
        this.listeningDeeplink = listeningDeeplink;
    }

    protected void setListeningPushInboxUpdate(boolean listeningPushInboxUpdate) {
        this.listeningPushInboxUpdate = listeningPushInboxUpdate;
    }

    private JSONObject embeddedMessageToJson(SwrveEmbeddedMessage message) throws Exception {
        JSONObject embeddedMessageJSON = new JSONObject();

        embeddedMessageJSON.put("campaignId", message.getCampaign().getId());
        embeddedMessageJSON.put("messageId", message.getId());
        embeddedMessageJSON.put("priority", message.getPriority());
        embeddedMessageJSON.put("type", message.getType());
        embeddedMessageJSON.put("data", message.getData());

        JSONArray buttons = new JSONArray();

        for (String button : message.getButtons()) {
            buttons.put(button);
        }
        embeddedMessageJSON.put("buttons", buttons);

        return embeddedMessageJSON;
    }

    JSONObject messageDetailsToJson(SwrveMessageDetails messageDetails) throws Exception {
        JSONObject messageDetailJson = new JSONObject();

        JSONArray buttons = new JSONArray();
        for (SwrveMessageButtonDetails button : messageDetails.getButtons()) {
            buttons.put(buttonDetailsToJson(button));
        }
        String campaignSubject = messageDetails.getCampaignSubject() == null ? "" : messageDetails.getCampaignSubject();
        messageDetailJson.put("campaignSubject", campaignSubject);

        messageDetailJson.put("campaignId", messageDetails.getCampaignId());
        messageDetailJson.put("variantId", messageDetails.getVariantId());

        String messageName = messageDetails.getMessageName() == null ? "" : messageDetails.getMessageName();
        messageDetailJson.put("messageName", messageName);
        messageDetailJson.put("buttons", buttons);

        return messageDetailJson;
    }

    private JSONObject buttonDetailsToJson(SwrveMessageButtonDetails buttonDetails) throws Exception {
        JSONObject selectedButton = new JSONObject();

        String buttonName = buttonDetails.getButtonName() == null ? "" : buttonDetails.getButtonName();
        selectedButton.put("buttonName", buttonName);

        String buttonText = buttonDetails.getButtonText() == null ? "" : buttonDetails.getButtonText();
        selectedButton.put("buttonText", buttonText);

        String actionType = buttonDetails.getActionType() == null ? "" : buttonDetails.getActionType().toString();
        selectedButton.put("actionType", actionType);

        String actionString = buttonDetails.getActionString() == null ? "" : buttonDetails.getActionString();
        selectedButton.put("actionString", actionString);
        return selectedButton;
    }
}
