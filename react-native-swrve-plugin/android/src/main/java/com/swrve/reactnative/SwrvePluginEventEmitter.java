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
import com.swrve.sdk.messaging.SwrveInAppMessageListener;
import com.swrve.sdk.messaging.SwrveMessageButtonDetails;
import com.swrve.sdk.messaging.SwrveMessageDetails;

import org.json.JSONArray;
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
    private final String CALLBACK_KEY_CUSTOM_BUTTON_ACTION_CAMPAIGN_NAME = "campaignName";
    private final String CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT = "campaignSubject";
    private final String CALLBACK_KEY_DISMISS_BUTTON_NAME = "buttonName";
    private final String CALLBACK_KEY_DISMISS_CAMPAIGN_NAME = "campaignName";
    private final String CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT = "clipboardContents";
    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP = "embeddedMessagePersonalizationProperties";

    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_MAP = "embeddedMessage";
    private final String CALLBACK_KEY_EMBEDDED_MESSAGE_OBJECT = "embeddedMessageObject";
    private final String CALLBACK_KEY_EMBEDDED_CONTROL_ACTION = "embeddedMessageIsControl";

    // InAppListener property
    private final String CALLBACK_KEY_MESSAGE_DETAIL_MAP = "messageDetail";
    private final String CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON = "messageDetailSelectedButton";
    private final String CALLBACK_KEY_MESSAGE_DETAIL_ACTION = "messageDetailAction";

    // Deeplink listener
    private final String CALLBACK_KEY_DEEPLINK_ACTION_STRING = "deeplinkDelegateActionString";

    // Event name (this is bing used by JS layer to listen)
    private final String RESOURCES_UPDATED_EVENT_NAME = "SwrveUserResourcesUpdated";
    private final String MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = "SwrveMessageCustomCallback";
    private final String MESSAGE_CALLBACK_DISMISS_EVENT_NAME = "SwrveMessageDismissCallback";
    private final String MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = "SwrveMessageClipboardCallback";
    private final String EMBEDDED_MESSAGE_CALLBACK_EVENT_NAME = "SwrveEmbeddedMessageCallback";
    private final String EMBEDDED_CALLBACK_EVENT_NAME = "SwrveEmbeddedMessageCallbackWithControlFlag";
    private final String MESSAGE_DETAILS_DELEGATE_EVENT_NAME = "SwrveMessageDetailsDelegate";
    private final String DEEPLINK_DELEGATE_EVENT_NAME = "SwrveMessageDeeplinkDelegate";

    private final String MODULE_NAME = "SwrvePluginEventEmitter";

    private boolean listeningCustom = false;
    private boolean listeningDeeplink = false;
    private boolean useEmbeddedWithControlCallback = false;
    private boolean listeningInAppMessageListener = false;

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

    public void onCustomAction(String customAction, String campaignName) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_CUSTOM_BUTTON_ACTION, customAction);
            params.putString(CALLBACK_KEY_CUSTOM_BUTTON_ACTION_CAMPAIGN_NAME, campaignName);
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

    public void onDismissAction(String campaignSubject, String buttonName, String campaignName) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT, campaignSubject);
            params.putString(CALLBACK_KEY_DISMISS_BUTTON_NAME, buttonName);
            params.putString(CALLBACK_KEY_DISMISS_CAMPAIGN_NAME, campaignName);
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

    public void onEmbeddedCallback(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties, Boolean isControl) {
        if(this.useEmbeddedWithControlCallback == false) {
            onEmbeddedMessageCallback(message, personalizationProperties);
            return;
        }

        try {
            WritableMap params = Arguments.createMap();

            JSONObject embeddedMessageJSON = embeddedMessageToJson(message);
            params.putMap(CALLBACK_KEY_EMBEDDED_MESSAGE_MAP, SwrvePluginUtils.convertJsonToMap(embeddedMessageJSON));

            if(personalizationProperties != null) {
                WritableMap personalizationMap = SwrvePluginUtils.convertJsonToMap(new JSONObject(personalizationProperties));
                params.putMap(CALLBACK_KEY_EMBEDDED_MESSAGE_PERSONALIZATION_PROPERTIES_MAP, personalizationMap);
            }
            String isControlFlag =  isControl ? "true" : "false";
            params.putString(CALLBACK_KEY_EMBEDDED_CONTROL_ACTION, isControlFlag);

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(EMBEDDED_CALLBACK_EVENT_NAME, params);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit embedded message triggered event", e);
        }
    }

    public void onMessageDetailDelegate(
            SwrveInAppMessageListener.SwrveMessageAction action,
            SwrveMessageDetails messageDetails,
            SwrveMessageButtonDetails selectedButton
    ) {
        if (listeningInAppMessageListener == false) {
            switch (action) {
                case Dismiss:
                    onDismissAction(messageDetails.getCampaignSubject(), selectedButton.getButtonName(), messageDetails.getMessageName());
                    break;
                    case Custom:
                        onCustomAction(selectedButton.getActionString(), messageDetails.getMessageName());
                        break;
                    case CopyToClipboard:
                        onClipboardAction(selectedButton.getActionString());
                        break;
                    default:
                        break;
                }
            return;
        }

        try {
            WritableMap params = Arguments.createMap();

            params.putString(CALLBACK_KEY_MESSAGE_DETAIL_ACTION, action.toString());

            JSONObject json = messageDetailsToJson(messageDetails);
            params.putMap(CALLBACK_KEY_MESSAGE_DETAIL_MAP, SwrvePluginUtils.convertJsonToMap(json));

            if(selectedButton != null) {
                WritableMap buttonMap = SwrvePluginUtils.convertJsonToMap(buttonDetailsToJson(selectedButton));
                params.putMap(CALLBACK_KEY_MESSAGE_DETAIL_SELECTED_BUTTON, buttonMap);
            }

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(MESSAGE_DETAILS_DELEGATE_EVENT_NAME, params);

            } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to  emit swrve message details triggered event", e);
        }
    }

     public void onDeeplinkDelegate(String actionString) {
        if(this.listeningDeeplink == false) {
            processAction(actionString);
            return;
            }

        try {
            WritableMap params = Arguments.createMap();

            params.putString(CALLBACK_KEY_DEEPLINK_ACTION_STRING, actionString);

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(DEEPLINK_DELEGATE_EVENT_NAME, params);

            } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to  emit swrve message details triggered event", e);
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

    public void setListeningDeeplink(boolean listeningDeeplink) {
        this.listeningDeeplink = listeningDeeplink;
    }

    public void setEmbeddedWithControlCallback(boolean useEmbeddedWithControlCallback) {
        this.useEmbeddedWithControlCallback = useEmbeddedWithControlCallback;
    }

    public void setListeningInAppMessageListener(boolean listeningInAppMessageListener) {
        this.listeningInAppMessageListener = listeningInAppMessageListener;
    }

    private JSONObject embeddedMessageToJson(SwrveEmbeddedMessage message) throws Exception {
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
    
    JSONObject messageDetailsToJson(SwrveMessageDetails messageDetails) throws Exception {
        JSONObject messageDetailJson = new JSONObject();

        JSONArray buttons = new JSONArray();
        for (SwrveMessageButtonDetails button : messageDetails.getButtons()) {
            buttons.put(buttonDetailsToJson(button));
        }
        String campaignSubject =  messageDetails.getCampaignSubject() == null ? "" : messageDetails.getCampaignSubject();
        messageDetailJson.put("campaignSubject", campaignSubject);

        messageDetailJson.put("campaignId", messageDetails.getCampaignId());
        messageDetailJson.put("variantId", messageDetails.getVariantId());

        String messageName =  messageDetails.getMessageName() == null ? "" : messageDetails.getMessageName();
        messageDetailJson.put("messageName", messageName);
        messageDetailJson.put("buttons", buttons);

        return messageDetailJson;
    }

    private JSONObject buttonDetailsToJson(SwrveMessageButtonDetails buttonDetails) throws Exception {
        JSONObject selectedButton = new JSONObject();

        String buttonName =  buttonDetails.getButtonName() == null ? "" : buttonDetails.getButtonName();
        selectedButton.put("buttonName", buttonName);

        String buttonText =  buttonDetails.getButtonText() == null ? "" : buttonDetails.getButtonText();
        selectedButton.put("buttonText", buttonText);

        String actionType =  buttonDetails.getActionType() == null ? "" : buttonDetails.getActionType().toString();
        selectedButton.put("actionType", actionType);

        String actionString =  buttonDetails.getActionString() == null ? "" : buttonDetails.getActionString();
        selectedButton.put("actionString", actionString);
        return selectedButton;
    }
}