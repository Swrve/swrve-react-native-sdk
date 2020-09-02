package com.swrvegeo.reactnative;

import android.app.Application;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.swrve.sdk.geo.SwrveGeoConfig;
import com.swrve.sdk.geo.SwrveGeoSDK;

public class SwrveGeoPluginModule extends ReactContextBaseJavaModule {

    final static String LOG_TAG = "SwrveGeoPluginModule";
    public static String SWRVE_GEO_PLUGIN_VERSION = "1.0.0";
    private final String MODULE_NAME = "SwrveGeoPlugin";
    private final ReactApplicationContext reactContext;

    public SwrveGeoPluginModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    static void init(Application application, SwrveGeoConfig config) {
        if (config != null) {
            SwrveGeoSDK.init(application.getApplicationContext(), config);
        } else {
            SwrveGeoSDK.init(application.getApplicationContext());
        }
    }

    @ReactMethod
    public void start() {
        SwrveGeoSDK.start(this.reactContext.getCurrentActivity());
    }

    @ReactMethod
    public void stop() {
        SwrveGeoSDK.stop(this.reactContext);
    }

    @ReactMethod
    public void isStarted(final Promise promise) {
        try {
            Boolean isStarted = SwrveGeoSDK.isStarted(this.reactContext);
            promise.resolve(isStarted);
        } catch (RuntimeException runtime) {
            promise.reject("EXCEPTION", runtime.toString());
        }
    }

    @ReactMethod
    public void getVersion(final Promise promise) {
        try {
            String version = SwrveGeoSDK.getVersion();
            promise.resolve(version);
        } catch (RuntimeException runtime) {
            promise.reject("EXCEPTION", runtime.toString());
        }
    }
}
