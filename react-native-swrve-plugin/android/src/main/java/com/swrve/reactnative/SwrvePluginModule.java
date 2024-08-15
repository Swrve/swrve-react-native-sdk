package com.swrve.reactnative;

import static com.swrve.reactnative.SwrvePluginErrorCodes.EXCEPTION;
import static com.swrve.reactnative.SwrvePluginErrorCodes.INVALID_ARGUMENT;
import static com.swrve.reactnative.SwrvePluginErrorCodes.swrveResponseCode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.gson.Gson;
import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveIdentityResponse;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveRealTimeUserPropertiesListener;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveUserResourcesDiffListener;
import com.swrve.sdk.SwrveUserResourcesListener;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SwrvePluginModule extends ReactContextBaseJavaModule {

    public static String SWRVE_PLUGIN_VERSION = "5.0.1";
    private final ReactApplicationContext reactContext;
    private final SwrvePluginEventEmitter eventEmitter;

    SwrvePluginModule(ReactApplicationContext reactContext, @NonNull SwrvePluginEventEmitter eventEmitter) {
        super(reactContext);
        this.reactContext = reactContext;
        this.eventEmitter = eventEmitter;
        SwrveListeners.getInstance().setEventEmitter(eventEmitter);
    }

    @NonNull
    @Override
    public String getName() {
        return "SwrvePlugin";
    }

    @ReactMethod
    public void start(final @Nullable String userId) {
        SwrveLogger.v("SwrvePlugin: start()");
        if (userId != null && !userId.isEmpty()) {
            SwrveSDK.start(getCurrentActivity(), userId);
        } else {
            SwrveSDK.start(getCurrentActivity());
        }
    }

    @ReactMethod
    public void identify(final String userIdentity, final Promise promise) {
        SwrveLogger.v("SwrvePlugin: identify()");
        if (userIdentity == null || userIdentity.isEmpty()) {
            SwrveLogger.e("SwrvePlugin: No user identity supplied");
            promise.reject(INVALID_ARGUMENT, "identify() - No user identity supplied");
        }

        SwrveSDK.identify(userIdentity, new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
                SwrveLogger.v("SwrvePlugin: identify() - success");
                promise.resolve(swrveId);
            }

            @Override
            public void onError(int responseCode, String errorMessage) {
                SwrveLogger.e("SwrvePlugin: identify() - error " + errorMessage);
                promise.reject(swrveResponseCode(responseCode), errorMessage);
            }
        });
    }

    @ReactMethod
    public void userUpdate(final ReadableMap attributes) {
        SwrveLogger.v("SwrvePlugin: userUpdate()");
        SwrveSDK.userUpdate(SwrvePluginUtils.convertToStringMap(attributes));
    }

    @ReactMethod
    public void userUpdateDate(final String name, final String date) {
        SwrveLogger.v("SwrvePlugin: userUpdateDate()");
        try {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            df.setTimeZone(tz);
            final Date finalDate = df.parse(date);

            SwrveSDK.userUpdate(name, finalDate);
        } catch (ParseException e) {
            SwrveLogger.e("SwrvePlugin: Unable to parse date in userUpdateDate", e);
        }
    }

    @ReactMethod
    public void event(final String eventName, final ReadableMap eventPayload) {
        if (eventPayload == null) {
            SwrveLogger.v("SwrvePlugin: event()");
            SwrveSDK.event(eventName);
        } else {
            // Take everything from the eventPayload that can be converted to a string
            SwrveLogger.v("SwrvePlugin: event(payload)");
            SwrveSDK.event(eventName, SwrvePluginUtils.convertToStringMap(eventPayload));
        }
    }

    @ReactMethod
    public void sendQueuedEvents() {
        SwrveLogger.v("SwrvePlugin: sendQueuedEvents()");
        SwrveSDK.sendQueuedEvents();
    }

    @ReactMethod
    public void currencyGiven(final String currency, int quantity) {
        SwrveLogger.v("SwrvePlugin: currencyGiven()");
        SwrveSDK.currencyGiven(currency, quantity);
    }

    @ReactMethod
    public void purchase(final String itemName, final String currency, int quantity, int cost) {
        SwrveLogger.v("SwrvePlugin: purchase()");
        SwrveSDK.purchase(itemName, currency, cost, quantity);
    }

    @ReactMethod
    public void unvalidatedIap(double localCost, final String localCurrency, final String productId, int quantity) {
        SwrveLogger.v("SwrvePlugin: unvalidatedIap()");
        SwrveSDK.iap(quantity, productId, localCost, localCurrency);
    }

    @ReactMethod
    public void unvalidatedIapWithReward(double localCost, final String localCurrency, final String productId, int quantity, ReadableMap rewardMap) {
        SwrveLogger.v("SwrvePlugin: unvalidatedIapWithReward()");
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

        SwrveSDK.iap(quantity, productId, localCost, localCurrency, rewards);
    }

    @ReactMethod
    public void getApiKey(final Promise promise) {
        try {
            String apiKey = SwrveSDK.getApiKey();
            promise.resolve(apiKey);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getUserId(final Promise promise) {
        try {
            String userId = SwrveSDK.getUserId();
            promise.resolve(userId);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getExternalUserId(final Promise promise) {
        try {
            String externalUserId = SwrveSDK.getExternalUserId();
            promise.resolve(externalUserId);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getUserResources(final Promise promise) {
        try {
            SwrveSDK.getUserResources(new SwrveUserResourcesListener() {
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
            SwrveSDK.getUserResourcesDiff(new SwrveUserResourcesDiffListener() {
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
            Boolean isStarted = SwrveSDK.isStarted();
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
                campaigns = SwrveSDK.getMessageCenterCampaigns(SwrvePluginUtils.convertToStringMap(personalization));
            } else {
                campaigns = SwrveSDK.getMessageCenterCampaigns();
            }
            JSONArray result = new JSONArray();
            for (SwrveBaseCampaign campaign : campaigns) {
                JSONObject campaignJSON = mapSwrveBaseCampaignToMessageCenterJSON(campaign);
                result.put(campaignJSON);
            }
            WritableArray messageCenterCampaigns = SwrvePluginUtils.convertJsonToArray(result);
            promise.resolve(messageCenterCampaigns);

        } catch (Exception runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getMessageCenterCampaign(int campaignId, final ReadableMap personalization, final Promise promise) {
        SwrveBaseCampaign campaign = SwrveSDK.getMessageCenterCampaign(campaignId, SwrvePluginUtils.convertToStringMap(personalization));
        try {
            if (campaign != null) {
                JSONObject campaignJSON = mapSwrveBaseCampaignToMessageCenterJSON(campaign);
                WritableMap messageCenterCampaign = SwrvePluginUtils.convertJsonToMap(campaignJSON);
                promise.resolve(messageCenterCampaign);
            } else {
                promise.reject(EXCEPTION, "Unable to find campaign");
            }
        } catch (Exception runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void showMessageCenterCampaign(int campaignId, ReadableMap personalization) {
        SwrveBaseCampaign candidateCampaign = findMessageCenterCampaignbyID(campaignId);
        if (candidateCampaign != null) {
            if (personalization != null) {
                SwrveSDK.showMessageCenterCampaign(candidateCampaign,
                        SwrvePluginUtils.convertToStringMap(personalization));
            } else {
                SwrveSDK.showMessageCenterCampaign(candidateCampaign);
            }
        } else {
            SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
        }
    }

    @ReactMethod
    public void removeMessageCenterCampaign(int campaignId) {
        SwrveBaseCampaign canditateCampaign = findMessageCenterCampaignbyID(campaignId);
        if (canditateCampaign != null) {
            SwrveSDK.removeMessageCenterCampaign(canditateCampaign);
        } else {
            SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
        }
    }

    @ReactMethod
    public void markMessageCenterCampaignAsSeen(int campaignId) {
        SwrveBaseCampaign canditateCampaign = findMessageCenterCampaignbyID(campaignId);
        if (canditateCampaign != null) {
            SwrveSDK.markMessageCenterCampaignAsSeen(canditateCampaign);
        } else {
            SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
        }
    }

    @ReactMethod
    public void getRealTimeUserProperties(final Promise promise) {
        try {
            SwrveSDK.getRealTimeUserProperties(new SwrveRealTimeUserPropertiesListener() {
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
    public void markEmbeddedMessageCampaignAsSeen(int campaignId) {
        SwrveEmbeddedCampaign EmbeddedCampaign = findEmbeddedCampaignByID(campaignId);
        if (EmbeddedCampaign != null) {
            SwrveSDK.embeddedMessageWasShownToUser(EmbeddedCampaign.getMessage());
        } else {
            SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
        }
    }

    @ReactMethod
    public void markEmbeddedMessageButtonAsPressed(int campaignId, String buttonName) {
        SwrveEmbeddedCampaign EmbeddedCampaign = findEmbeddedCampaignByID(campaignId);
        if (EmbeddedCampaign != null) {
            SwrveSDK.embeddedMessageButtonWasPressed(EmbeddedCampaign.getMessage(), buttonName);
        } else {
            SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
        }
    }

    @ReactMethod
    public void getPersonalizedText(String text, ReadableMap personalizationProperties, final Promise promise) {
        try {
            String personalizedText = SwrveSDK.getPersonalizedText(text, SwrvePluginUtils.convertToStringMap(personalizationProperties));
            promise.resolve(personalizedText);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void getPersonalizedEmbeddedMessageData(int campaignId, ReadableMap personalizationProperties, final Promise promise) {
        try {
            SwrveEmbeddedCampaign EmbeddedCampaign = findEmbeddedCampaignByID(campaignId);
            String personalizedEmbeddedMessageData = "";

            if (EmbeddedCampaign != null) {
                personalizedEmbeddedMessageData = SwrveSDK.getPersonalizedEmbeddedMessageData(EmbeddedCampaign.getMessage(), SwrvePluginUtils.convertToStringMap(personalizationProperties));
            } else {
                SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
            }

            promise.resolve(personalizedEmbeddedMessageData);
        } catch (RuntimeException runtime) {
            promise.reject(EXCEPTION, runtime.toString());
        }
    }

    @ReactMethod
    public void embeddedControlMessageImpressionEvent(int campaignId) {
        SwrveEmbeddedCampaign EmbeddedCampaign = findEmbeddedCampaignByID(campaignId);
        if (EmbeddedCampaign != null) {
            SwrveSDK.embeddedControlMessageImpressionEvent(EmbeddedCampaign.getMessage());
        } else {
            SwrveLogger.e("SwrvePlugin: Unable to find campaign of id:%s", campaignId);
        }
    }

    @ReactMethod
    public void refreshCampaignsAndResources() {
        SwrveSDK.refreshCampaignsAndResources();
    }

    @ReactMethod
    public void stopTracking() {
        SwrveSDK.stopTracking();
    }

    @ReactMethod
    public void startedListening() {
        SwrveListeners.getInstance().setShouldBufferEvents(false);
        SwrveListeners.getInstance().flushBufferedEvents(reactContext);
    }

    @ReactMethod
    public void startedEmbeddedListening() {
        SwrveLogger.v("SwrvePlugin: startedEmbeddedListening()");
        SwrveListeners.getInstance().setShouldBufferEmbeddedListenerEvents(false);
        SwrveListeners.getInstance().flushBufferedEmbeddedListenerEvents();
    }

    @ReactMethod
    public void stoppedListening() {
        SwrveListeners.getInstance().setShouldBufferEvents(true);
    }

    @ReactMethod
    public void listeningDeeplink() {
        eventEmitter.setListeningDeeplink(true);
    }

    // Private Methods
    private JSONObject mapSwrveBaseCampaignToMessageCenterJSON(SwrveBaseCampaign campaign) throws JSONException {
        JSONObject campaignJSON = new JSONObject();
        campaignJSON.put("ID", campaign.getId());
        campaignJSON.put("name", campaign.getName());
        campaignJSON.put("maxImpressions", campaign.getMaxImpressions());
        campaignJSON.put("priority", (campaign.getPriority()));
        campaignJSON.put("downloadDate", (campaign.getDownloadDate().getTime() / 1000));
        campaignJSON.put("dateStart", (campaign.getStartDate().getTime() / 1000));
        campaignJSON.put("dateEnd", (campaign.getEndDate().getTime() / 1000));
        campaignJSON.put("messageCenter", campaign.isMessageCenter());
        campaignJSON.put("state", campaign.getSaveableState().toJSON());

        if (campaign.getMessageCenterDetails() != null) {
            JSONObject messageCentreJSON = new JSONObject();
            String subject = campaign.getMessageCenterDetails().getSubject();
            String description = campaign.getMessageCenterDetails().getDescription();
            String imageAccessibilityText = campaign.getMessageCenterDetails().getImageAccessibilityText();
            String imageSha = campaign.getMessageCenterDetails().getImageSha();
            String imageUrl = campaign.getMessageCenterDetails().getImageURL();
            String imagePath = null;

            if (subject != null) {
                messageCentreJSON.put("subject", subject);
            }
            if (description != null) {
                messageCentreJSON.put("description", description);
            }
            if (imageAccessibilityText != null) {
                messageCentreJSON.put("imageAccessibilityText", imageAccessibilityText);
            }

            if (imageUrl != null) {
                messageCentreJSON.put("imageURL", imageUrl);

                String imageURLSha = SwrveHelper.sha1(imageUrl.getBytes());
                File imageFile = getFilePathForAsset(imageURLSha);

                if (imageFile != null) {
                    imagePath = imageFile.getAbsolutePath();
                }
            }

            if (imageSha != null) {
                messageCentreJSON.put("imageSha", imageSha);

                if (imagePath == null) {
                    File imageFile = getFilePathForAsset(imageSha);
                    if (imageFile != null) {
                        imagePath = imageFile.getAbsolutePath();
                    }
                }
            }

            if (imagePath != null) {
                messageCentreJSON.put("image", "file://" + imagePath);
            }
            campaignJSON.put("messageCenterDetails", messageCentreJSON);
        }
        return campaignJSON;
    }

    private SwrveBaseCampaign findMessageCenterCampaignbyID(int identifier) {
        List<SwrveBaseCampaign> campaigns = SwrveSDK.getMessageCenterCampaigns();
        SwrveBaseCampaign canditateCampaign = null;

        for (SwrveBaseCampaign campaign : campaigns) {
            if (campaign.getId() == identifier) {
                canditateCampaign = campaign;
            }
        }

        return canditateCampaign;
    }

    private String getCache() {
        Swrve swrve = (Swrve) SwrveSDK.getInstance();
        return swrve.getCachedData(swrve.getUserId(), swrve.CACHE_CAMPAIGNS);
    }

    private File getFilePathForAsset(String imageSha) {
        Swrve swrve = (Swrve) SwrveSDK.getInstance();
        File asset = null;
        File file = new File(swrve.getCacheDir() + "/" + imageSha);
        if (file.exists()) {
            asset = file;
        }
        return asset;
    }

    private SwrveEmbeddedCampaign findEmbeddedCampaignByID(int identifier) {
        String cacheStr = getCache();
        SwrveEmbeddedCampaign canditateCampaign = null;

        try {
            JSONObject cache = new JSONObject(cacheStr);
            JSONArray campaigns = cache.getJSONArray("campaigns");

            for (int i = 0; i < campaigns.length(); i++) {
                JSONObject campaignData = campaigns.getJSONObject(i);
                if (campaignData.getInt("id") == identifier) {
                    canditateCampaign = new SwrveEmbeddedCampaign((ISwrveCampaignManager) SwrveSDK.getInstance(), new SwrveCampaignDisplayer(), campaignData);
                }
            }
        } catch (Exception e) {
            SwrveLogger.e("SwrvePlugin: Unable to access cache: %s", e, identifier);
        }
        return canditateCampaign;
    }
}
