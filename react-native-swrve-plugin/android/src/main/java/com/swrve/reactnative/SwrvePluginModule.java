package com.swrve.reactnative;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.google.gson.Gson;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveIdentityResponse;
import com.swrve.sdk.SwrvePushNotificationListener;
import com.swrve.sdk.SwrveUserResourcesDiffListener;
import com.swrve.sdk.SwrveUserResourcesListener;
import com.swrve.sdk.SwrveResourcesListener;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveSilentPushListener;
import com.swrve.sdk.SwrveRealTimeUserPropertiesListener;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveClipboardButtonListener;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.swrve.reactnative.SwrvePluginErrorCodes.*;

@SuppressLint("LogNotTimber")
public class SwrvePluginModule extends ReactContextBaseJavaModule {
    final static String LOG_TAG = "SwrvePluginModule";
    static SwrveListenerDelegateHolder delegateHolder = new SwrveListenerDelegateHolder();

    public static String SWRVE_PLUGIN_VERSION = "1.1.0";
    private final String MODULE_NAME = "SwrvePlugin";
    private final ReactApplicationContext reactContext;

    /**
     * Owns the listeners statically so that the events can be passed to the dynamic
     * event emitter
     */
    static class SwrveListenerDelegateHolder
            implements SwrveSilentPushListener, SwrvePushNotificationListener, SwrveResourcesListener {
        SwrvePluginEventEmitter delegate = null;
        boolean shouldBufferEvents = true;
        List<JSONObject> pushEventBuffer = new LinkedList<>();
        List<JSONObject> silentPushEventBuffer = new LinkedList<>();

        void resetStateForTesting() {
            delegate = null;
            shouldBufferEvents = true;
            pushEventBuffer = new LinkedList<>();
            silentPushEventBuffer = new LinkedList<>();
        }

        void setShouldBufferEvents(boolean shouldBufferEvents) {
            this.shouldBufferEvents = shouldBufferEvents;
        }

        void flushBufferedEvents(Context context) {
            Log.d(LOG_TAG,
                    "flushBufferedEvents() push " + pushEventBuffer.size() + " silent " + silentPushEventBuffer.size());
            if (delegate == null) {
                return;
            }

            for (JSONObject event : pushEventBuffer) {
                delegate.onPushNotification(event);
            }
            pushEventBuffer.clear();

            for (JSONObject event : silentPushEventBuffer) {
                delegate.onSilentPush(context, event);
            }
            silentPushEventBuffer.clear();
        }

        @Override
        public void onPushNotification(JSONObject payload) {
            if (delegate != null && !shouldBufferEvents) {
                Log.d(LOG_TAG, "onPushNotification() passing through");
                delegate.onPushNotification(payload);
            } else {
                Log.d(LOG_TAG, "onPushNotification() buffered");
                pushEventBuffer.add(payload);
            }
        }

        @Override
        public void onSilentPush(Context context, JSONObject payload) {
            if (delegate != null && !shouldBufferEvents) {
                Log.d(LOG_TAG, "onSilentPush() passing through");
                delegate.onSilentPush(context, payload);
            } else {
                Log.d(LOG_TAG, "onSilentPush() buffered");
                silentPushEventBuffer.add(payload);
            }
        }

        @Override
        public void onResourcesUpdated() {
            if (delegate != null) {
                Log.d(LOG_TAG, "userResources()");
                delegate.onResourcesUpdated();
            }
        }
    }

    ISwrveBase swrveInstance = null;

    // Test-only members
    Activity mockedCurrentActivity = null;

    SwrvePluginModule(ReactApplicationContext reactContext, @NonNull SwrvePluginEventEmitter eventEmitter) {
        super(reactContext);
        this.reactContext = reactContext;
        delegateHolder.delegate = eventEmitter;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    // Allow us to mock the current activity from the unit tests
    private Activity getActivity() {
        if (mockedCurrentActivity != null)
            return mockedCurrentActivity;
        else
            return getCurrentActivity();
    }

    private ISwrveBase getSwrveInstance() {
        if (swrveInstance == null) {
            swrveInstance = SwrveSDK.getInstance();
        }

        return swrveInstance;
    }

    // From the native-side of the app, called from SwrvePlugin
    static void createInstance(Application application, int appId, String apiKey, SwrveConfig config) {
        // Overload any listeners with our own static listener
        config.setSilentPushListener(delegateHolder);
        config.setNotificationListener(delegateHolder);

        // Override all inApp callbacks for button action listeners
        SwrveInAppMessageConfig inAppConfig = setupInAppMessageConfig(config);
        config.setInAppMessageConfig(inAppConfig);

        SwrveSDK.createInstance(application, appId, apiKey, config);
        sendPluginVersion();
    }

    @ReactMethod
    public void start(final @Nullable String userId) {
        Log.d(LOG_TAG, "start()");
        if (userId != null && !userId.isEmpty()) {
            getSwrveInstance().start(getActivity(), userId);
        } else {
            getSwrveInstance().start(getActivity());
        }

        if (getSwrveInstance().isStarted()) {
            sendPluginVersion();
        }
    }

    @ReactMethod
    public void identify(final String userIdentity, final Promise promise) {
        Log.d(LOG_TAG, "identify()");
        if (userIdentity == null || userIdentity.isEmpty()) {
            Log.e(LOG_TAG, "No user identity supplied");
            promise.reject(INVALID_ARGUMENT, "identify() - No user identity supplied");
        }

        getSwrveInstance().identify(userIdentity, new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
                Log.d(LOG_TAG, "identify() - success");
                promise.resolve(swrveId);
            }

            @Override
            public void onError(int responseCode, String errorMessage) {
                Log.e(LOG_TAG, "identify() - error " + errorMessage);
                promise.reject(swrveResponseCode(responseCode), errorMessage);
            }
        });
    }

    @ReactMethod
    public void userUpdate(final ReadableMap attributes) {
        Log.d(LOG_TAG, "userUpdate()");
        getSwrveInstance().userUpdate(SwrvePluginUtils.convertToStringMap(attributes));
    }

    @ReactMethod
    public void userUpdateDate(final String name, final String date) {
        Log.d(LOG_TAG, "userUpdateDate()");

        try {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            df.setTimeZone(tz);
            final Date finalDate = df.parse(date);

            getSwrveInstance().userUpdate(name, finalDate);
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Unable to parse date in userUpdateDate", e);
        }
    }

    @ReactMethod
    public void event(final String eventName, final ReadableMap eventPayload) {
        if (eventPayload == null) {
            Log.d(LOG_TAG, "event()");
            getSwrveInstance().event(eventName);
        } else {
            // Take everything from the eventPayload that can be converted to a string
            Log.d(LOG_TAG, "event(payload)");
            getSwrveInstance().event(eventName, SwrvePluginUtils.convertToStringMap(eventPayload));
        }
    }

    @ReactMethod
    public void sendQueuedEvents() {
        Log.d(LOG_TAG, "sendQueuedEvents()");
        getSwrveInstance().sendQueuedEvents();
    }

    @ReactMethod
    public void currencyGiven(final String currency, int quantity) {
        Log.d(LOG_TAG, "currencyGiven()");
        getSwrveInstance().currencyGiven(currency, quantity);
    }

    @ReactMethod
    public void purchase(final String itemName, final String currency, int quantity, int cost) {
        Log.d(LOG_TAG, "purchase()");
        getSwrveInstance().purchase(itemName, currency, cost, quantity);
    }

    @ReactMethod
    public void unvalidatedIap(double localCost, final String localCurrency, final String productId, int quantity) {
        Log.d(LOG_TAG, "unvalidatedIap()");
        getSwrveInstance().iap(quantity, productId, localCost, localCurrency);
    }

    @ReactMethod
    public void unvalidatedIapWithReward(double localCost, final String localCurrency, final String productId,
            int quantity, ReadableMap rewardMap) {
        Log.d(LOG_TAG, "unvalidatedIapWithReward()");
        SwrveIAPRewards rewards = new SwrveIAPRewards();

        // Loop through the items and add to the rewards
        if (rewardMap.hasKey("items") && rewardMap.getType("items") == ReadableType.Array) {
            ReadableArray rewardArray = rewardMap.getArray("items");
            for (int i = 0; i < rewardArray.size(); i++) {
                if (rewardArray.getType(0) == ReadableType.Map) {
                    ReadableMap rewardDetails = rewardArray.getMap(i);
                    rewards.addItem(rewardDetails.getString("name"), rewardDetails.getInt("amount"));
                }
            }
        }

        // Loop through the currencies and add to the rewards
        if (rewardMap.hasKey("currencies") && rewardMap.getType("currencies") == ReadableType.Array) {
            ReadableArray rewardArray = rewardMap.getArray("currencies");
            for (int i = 0; i < rewardArray.size(); i++) {
                if (rewardArray.getType(0) == ReadableType.Map) {
                    ReadableMap rewardDetails = rewardArray.getMap(i);
                    rewards.addCurrency(rewardDetails.getString("name"), rewardDetails.getInt("amount"));
                }
            }
        }

        getSwrveInstance().iap(quantity, productId, localCost, localCurrency, rewards);
    }

    @ReactMethod
    public void getApiKey(final Promise promise) {
        try {
            String apiKey = getSwrveInstance().getApiKey();
            promise.resolve(apiKey);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getUserId(final Promise promise) {
        try {
            String userId = getSwrveInstance().getUserId();
            promise.resolve(userId);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getExternalUserId(final Promise promise) {
        try {
            String externalUserId = getSwrveInstance().getExternalUserId();
            promise.resolve(externalUserId);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getUserResources(final Promise promise) {
        try {
            getSwrveInstance().getUserResources(new SwrveUserResourcesListener() {
                @Override
                public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
                    try {
                        Gson gsonObj = new Gson();
                        String jsonStr = gsonObj.toJson(resources);
                        WritableMap resourcesMap = SwrvePluginUtils.convertJsonToMap(new JSONObject(jsonStr));
                        promise.resolve(resourcesMap);

                    } catch (JSONException e) {
                        promise.reject(EXCEPTION, e.toString());
                    }
                }

                @Override
                public void onUserResourcesError(Exception exception) {
                    promise.reject(EXCEPTION, exception.toString());
                }
            });

        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getUserResourcesDiff(final Promise promise) {
        try {
            getSwrveInstance().getUserResourcesDiff(new SwrveUserResourcesDiffListener() {
                @Override
                public void onUserResourcesDiffSuccess(Map<String, Map<String, String>> oldResourcesValues,
                        Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {

                    HashMap<String, Map<String, Map<String, String>>> userResourcesDiff = new HashMap();
                    userResourcesDiff.put("oldResourcesValues", oldResourcesValues);
                    userResourcesDiff.put("newResourcesValues", newResourcesValues);
                    try {
                        Gson gsonObj = new Gson();
                        String jsonStr = gsonObj.toJson(userResourcesDiff);
                        WritableMap resourcesMap = SwrvePluginUtils.convertJsonToMap(new JSONObject(jsonStr));
                        promise.resolve(resourcesMap);

                    } catch (JSONException e) {
                        promise.reject(EXCEPTION, e.toString());
                    }
                }

                @Override
                public void onUserResourcesDiffError(Exception exception) {
                    promise.reject(EXCEPTION, exception.toString());
                }
            });
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void isStarted(final Promise promise) {
        try {
            Boolean isStarted = getSwrveInstance().isStarted();
            promise.resolve(isStarted);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    // Message Center
    @ReactMethod
    public void getMessageCenterCampaigns(final ReadableMap personalization, final Promise promise) {
        try {
            List<SwrveBaseCampaign> campaigns;

            if (personalization != null) {
                campaigns = getSwrveInstance()
                        .getMessageCenterCampaigns(SwrvePluginUtils.convertToStringMap(personalization));
            } else {
                campaigns = getSwrveInstance().getMessageCenterCampaigns();
            }

            JSONArray result = new JSONArray();

            for (SwrveBaseCampaign campaign : campaigns) {
                JSONObject campaignJSON = new JSONObject();
                campaignJSON.put("ID", campaign.getId());
                campaignJSON.put("maxImpressions", campaign.getMaxImpressions());
                campaignJSON.put("subject", campaign.getSubject());
                campaignJSON.put("dateStart", (campaign.getStartDate().getTime() / 1000));
                campaignJSON.put("messageCenter", campaign.isMessageCenter());
                campaignJSON.put("state", campaign.getSaveableState().toJSON());
                result.put(campaignJSON);
            }

            WritableArray messsageCenterCampaigns = SwrvePluginUtils.convertJsonToArray(result);
            promise.resolve(messsageCenterCampaigns);

        } catch (Exception runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void showMessageCenterCampaign(int campaignId, ReadableMap personalization) {
        SwrveBaseCampaign candidateCampaign = findMessageCenterCampaignbyID(campaignId);
        if (candidateCampaign != null) {

            if (personalization != null) {
                getSwrveInstance().showMessageCenterCampaign(candidateCampaign,
                        SwrvePluginUtils.convertToStringMap(personalization));
            } else {
                getSwrveInstance().showMessageCenterCampaign(candidateCampaign);
            }

        } else {
            Log.e(LOG_TAG, "Unable to find campaign of id: " + campaignId);
        }
    }

    @ReactMethod
    public void removeMessageCenterCampaign(int campaignId) {
        SwrveBaseCampaign canditateCampaign = findMessageCenterCampaignbyID(campaignId);
        if (canditateCampaign != null) {
            getSwrveInstance().removeMessageCenterCampaign(canditateCampaign);
        } else {
            Log.e(LOG_TAG, "Unable to find campaign of id: " + campaignId);
        }
    }

    @ReactMethod
    public void markMessageCenterCampaignAsSeen(int campaignId) {
        SwrveBaseCampaign canditateCampaign = findMessageCenterCampaignbyID(campaignId);
        if (canditateCampaign != null) {
            getSwrveInstance().markMessageCenterCampaignAsSeen(canditateCampaign);
        } else {
            Log.e(LOG_TAG, "Unable to find campaign of id: " + campaignId);
        }
    }

    @ReactMethod
    public void getRealTimeUserProperties(final Promise promise) {
        try {
            getSwrveInstance().getRealTimeUserProperties(new SwrveRealTimeUserPropertiesListener() {
                @Override
                public void onRealTimeUserPropertiesSuccess(Map<String, String> properties, String propertiesAsJSON) {
                    try {
                        JSONObject json = new JSONObject(properties);
                        promise.resolve(SwrvePluginUtils.convertJsonToMap(json));

                    } catch (JSONException e) {
                        promise.reject(EXCEPTION, e.toString());
                    }
                }

                @Override
                public void onRealTimeUserPropertiesError(Exception exception) {
                    promise.reject(EXCEPTION, exception.toString());
                }
            });

        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void refreshCampaignsAndResources() {
        getSwrveInstance().refreshCampaignsAndResources();
    }

    // Listener Methods
    @ReactMethod
    public void startedListening() {
        delegateHolder.setShouldBufferEvents(false);
        delegateHolder.flushBufferedEvents(reactContext);
    }

    @ReactMethod
    public void stoppedListening() {
        delegateHolder.setShouldBufferEvents(true);
    }

    @ReactMethod
    public void listeningCustom() {
        delegateHolder.delegate.setListeningCustom(true);
    }

    @ReactMethod
    public void listeningInstall() {
        delegateHolder.delegate.setListeningInstall(true);
    }

    // Private Methods
    private SwrveBaseCampaign findMessageCenterCampaignbyID(int identifier) {

        List<SwrveBaseCampaign> campaigns = getSwrveInstance().getMessageCenterCampaigns();
        SwrveBaseCampaign canditateCampaign = null;

        for (SwrveBaseCampaign campaign : campaigns) {
            if (campaign.getId() == identifier) {
                canditateCampaign = campaign;
            }
        }

        return canditateCampaign;
    }

    private static void sendPluginVersion() {
        if (SwrveSDK.isStarted()) {
            Map<String, String> userUpdateWrapperVersion = new HashMap<>();
            userUpdateWrapperVersion.put("swrve.react_native_plugin_version", SWRVE_PLUGIN_VERSION);
            SwrveSDK.userUpdate(userUpdateWrapperVersion);
        }
    }

    private static SwrveInAppMessageConfig setupInAppMessageConfig(SwrveConfig config) {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder();

        // Previous Config (just in case it was passed as part of Application.java)
        SwrveInAppMessageConfig passedInAppConfig = config.getInAppMessageConfig();

        if (passedInAppConfig != null) {
            // All styling settings get retained
            inAppConfigBuilder.hideToolbar(passedInAppConfig.isHideToolbar());
            inAppConfigBuilder.clickColor(passedInAppConfig.getClickColor());
            inAppConfigBuilder.focusColor(passedInAppConfig.getFocusColor());
            inAppConfigBuilder.defaultBackgroundColor(passedInAppConfig.getDefaultBackgroundColor());
            inAppConfigBuilder.personalisedTextTypeface(passedInAppConfig.getPersonalisedTextTypeface());
            inAppConfigBuilder.personalisedTextBackgroundColor(passedInAppConfig.getPersonalisedTextBackgroundColor());
            inAppConfigBuilder.personalisedTextForegroundColor(passedInAppConfig.getPersonalisedTextForegroundColor());
        }

        // Set Install Button Listener
        if (passedInAppConfig.getInstallButtonListener() == null) {
            inAppConfigBuilder.installButtonListener(new SwrveInstallButtonListener() {
                @Override
                public boolean onAction(String appStoreLink) {
                    return delegateHolder.delegate.onInstallAction(appStoreLink);
                }
            });
        } else {
            inAppConfigBuilder.installButtonListener(passedInAppConfig.getInstallButtonListener());
        }

        // Set Custom Button Listener
        if (passedInAppConfig.getCustomButtonListener() == null) {
            inAppConfigBuilder.customButtonListener(new SwrveCustomButtonListener() {

                @Override
                public void onAction(String customAction) {
                    delegateHolder.delegate.onCustomAction(customAction);
                }
            });
        } else {
            inAppConfigBuilder.customButtonListener(passedInAppConfig.getCustomButtonListener());
        }

        // Set Dismiss Button Listener
        if (passedInAppConfig.getDismissButtonListener() == null) {
            inAppConfigBuilder.dismissButtonListener(new SwrveDismissButtonListener() {
                @Override
                public void onAction(String campaignSubject, String buttonName) {
                    delegateHolder.delegate.onDismissAction(campaignSubject, buttonName);
                }
            });
        } else {
            inAppConfigBuilder.dismissButtonListener(passedInAppConfig.getDismissButtonListener());
        }

        // Set Clipboard Button Listener
        if (passedInAppConfig.getClipboardButtonListener() == null) {
            inAppConfigBuilder.clipboardButtonListener(new SwrveClipboardButtonListener() {
                @Override
                public void onAction(String clipboardContents) {
                    delegateHolder.delegate.onClipboardAction(clipboardContents);
                }
            });
        } else {
            inAppConfigBuilder.clipboardButtonListener(passedInAppConfig.getClipboardButtonListener());
        }

        // Set Personalisation Provider if the passed in config has set it
        if (passedInAppConfig.getPersonalisationProvider() != null) {
            inAppConfigBuilder.personalisationProvider(passedInAppConfig.getPersonalisationProvider());
        }

        return inAppConfigBuilder.build();
    }

}
