package com.swrve.reactnative;

import android.app.Application;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

/**
 * This is the native-side interface to the Swrve native module
 */
public class SwrvePlugin {

    /**
     * Initialise the native module with your configuration details
     * @param application your application context
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @param config  your SwrveConfig options
     */
    public static void createInstance(Application application, int appId, String apiKey, SwrveConfig config) {
        SwrvePluginModule.createInstance(application, appId, apiKey, config);
    }

}
