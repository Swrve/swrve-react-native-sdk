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

import org.json.JSONObject;

import static com.swrve.reactnative.SwrvePluginModule.LOG_TAG;

@SuppressLint("LogNotTimber")
public class SwrvePluginEventEmitter extends ReactContextBaseJavaModule
        implements SwrvePushNotificationListener, SwrveSilentPushListener, SwrveResourcesListener {

    private final String PUSH_EVENT_NAME = "PushNotification";
    private final String SILENT_PUSH_EVENT_NAME = "SilentPushNotification";
    private final String PUSH_EVENT_PAYLOAD = "PushEventPayload";
    private final String CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL = "appStoreUrl";
    private final String CALLBACK_KEY_CUSTOM_BUTTON_ACTION = "customAction";
    private final String CALLBACK_KEY_DISMISS_BUTTON_CAMPAIGN_SUBJECT = "campaignSubject";
    private final String CALLBACK_KEY_DISMISS_BUTTON_NAME = "buttonName";
    private final String CALLBACK_KEY_CLIPBOARD_BUTTON_PROCESSED_TEXT = "clipboardContents";

    private final String RESOURCES_UPDATED_EVENT_NAME = "SwrveUserResourcesUpdated";
    private final String MESSAGE_CALLBACK_INSTALL_EVENT_NAME = "SwrveMessageInstallCallback";
    private final String MESSAGE_CALLBACK_CUSTOM_EVENT_NAME = "SwrveMessageCustomCallback";
    private final String MESSAGE_CALLBACK_DISMISS_EVENT_NAME = "SwrveMessageDismissCallback";
    private final String MESSAGE_CALLBACK_CLIPBOARD_EVENT_NAME = "SwrveMessageClipboardCallback";
    private final String MESSAGE_CALLBACK_PERSONALISATION_EVENT_NAME = "SwrveMessagePersonalisationCallback";
    private final String MODULE_NAME = "SwrvePluginEventEmitter";

    private boolean listeningCustom = false;
    private boolean listeningInstall = false;

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

    public boolean onInstallAction(String appStoreLink) {
        try {
            WritableMap params = Arguments.createMap();
            params.putString(CALLBACK_KEY_INSTALL_BUTTON_APPSTORE_URL, appStoreLink);
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(MESSAGE_CALLBACK_INSTALL_EVENT_NAME, params);

            if (this.listeningInstall == false) {
                processAction(appStoreLink);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to emit install button callback notification", e);
        }

        return true;
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

    public void setListeningInstall(boolean listeningInstall) {
        this.listeningInstall = listeningInstall;
    }
}
