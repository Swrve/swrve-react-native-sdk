package com.swrve.reactnative;

import android.app.Application;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePluginUserUpdate;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveEmbeddedMessageConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;

/**
 * This is the native-side interface to the Swrve native module
 */
public class SwrvePlugin {

    /**
     * Initialise the native module with your configuration details
     *
     * @param application your application context
     * @param appId       your app id in the Swrve dashboard
     * @param apiKey      your app api_key in the Swrve dashboard
     * @param config      your SwrveConfig options
     */
    public static void createInstance(Application application, int appId, String apiKey, SwrveConfig config) {

        // Overload any listeners with our own static listener
        SwrveListeners swrveListeners = SwrveListeners.getInstance();
        config.setSilentPushListener(swrveListeners);
        config.setNotificationListener(swrveListeners);

        // Override all inApp callbacks for button action listeners
        SwrveInAppMessageConfig inAppConfig = setupInAppMessageConfig(config);
        config.setInAppMessageConfig(inAppConfig);

        SwrveEmbeddedMessageConfig embeddedMessageConfig = setupEmbeddedMessageConfig(config);
        config.setEmbeddedMessageConfig(embeddedMessageConfig);

        setupDeeplinkListener(config);

        SwrveSDK.createInstance(application, appId, apiKey, config);
        SwrveSDK.setResourcesListener(swrveListeners);

        new SwrvePluginUserUpdate(application);
    }

    private static SwrveInAppMessageConfig setupInAppMessageConfig(SwrveConfig config) {

        SwrveListeners swrveListeners = SwrveListeners.getInstance();
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder();

        // Previous Config (just in case it was passed as part of Application.java)
        SwrveInAppMessageConfig passedInAppConfig = config.getInAppMessageConfig();

        if (passedInAppConfig != null) {
            // All styling settings get retained
            inAppConfigBuilder.hideToolbar(passedInAppConfig.isHideToolbar());
            inAppConfigBuilder.clickColor(passedInAppConfig.getClickColor());
            inAppConfigBuilder.messageFocusListener(passedInAppConfig.getMessageFocusListener());
            inAppConfigBuilder.defaultBackgroundColor(passedInAppConfig.getDefaultBackgroundColor());
            inAppConfigBuilder.personalizedTextTypeface(passedInAppConfig.getPersonalizedTextTypeface());
            inAppConfigBuilder.personalizedTextBackgroundColor(passedInAppConfig.getPersonalizedTextBackgroundColor());
            inAppConfigBuilder.personalizedTextForegroundColor(passedInAppConfig.getPersonalizedTextForegroundColor());
        }

        // Set Personalization Provider if the passed in config has set it
        if (passedInAppConfig.getPersonalizationProvider() != null) {
            inAppConfigBuilder.personalizationProvider(passedInAppConfig.getPersonalizationProvider());
        }

        // Set InApp Message Listener
        if (passedInAppConfig.getMessageListener() == null) {
            inAppConfigBuilder.messageListener((context, action, messageDetails, selectedButton) -> {
                if (swrveListeners.pluginEventEmitter != null) {
                    swrveListeners.pluginEventEmitter.onMessageDetailDelegate(action, messageDetails, selectedButton);
                }
            });
        } else {
            inAppConfigBuilder.messageListener(passedInAppConfig.getMessageListener());
        }

        return inAppConfigBuilder.build();
    }

    private static SwrveEmbeddedMessageConfig setupEmbeddedMessageConfig(SwrveConfig config) {
        SwrveEmbeddedMessageConfig embeddedMessageConfig = config.getEmbeddedMessageConfig();
        if (embeddedMessageConfig != null && embeddedMessageConfig.getEmbeddedListener() != null) {
            // don't override customers embeddedMessageConfig listener
        } else {
            SwrveEmbeddedMessageConfig.Builder embeddedMessageConfigBuilder = new SwrveEmbeddedMessageConfig.Builder();
            embeddedMessageConfigBuilder.embeddedListener(SwrveListeners.getInstance());
            embeddedMessageConfig = embeddedMessageConfigBuilder.build();
        }
        return embeddedMessageConfig;
    }

    private static void setupDeeplinkListener(SwrveConfig config) {
        if (config.getSwrveDeeplinkListener() == null) {
            SwrveListeners swrveListeners = SwrveListeners.getInstance();
            config.setSwrveDeeplinkListener((context, uriString, extras) -> {
                if (swrveListeners.pluginEventEmitter != null) {
                    swrveListeners.pluginEventEmitter.onDeeplinkDelegate(uriString);
                } else {
                    SwrveLogger.w("SwrvePlugin: Unable to handle opening deeplink in react native as it may not be ready. Attempting to open deeplink directly.");
                    SwrvePluginUtils.openDeepLink(context, uriString, extras);
                }
            });
        }
    }
}
