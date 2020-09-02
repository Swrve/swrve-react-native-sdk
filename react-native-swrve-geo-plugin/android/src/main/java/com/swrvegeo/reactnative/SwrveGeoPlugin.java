package com.swrvegeo.reactnative;

import android.app.Application;

import com.swrve.sdk.geo.SwrveGeoConfig;
import com.swrve.sdk.geo.SwrveGeoSDK;

/**
 * This is the native-side interface to the Swrve native module
 */
public class SwrveGeoPlugin {

    /**
     * Initialise the native module with your configuration details
     * 
     * @param application your application context
     * @param config      your SwrveGeoConfig options
     */
    public static void init(Application application, SwrveGeoConfig config) {
        SwrveGeoPluginModule.init(application, config);
    }
}
