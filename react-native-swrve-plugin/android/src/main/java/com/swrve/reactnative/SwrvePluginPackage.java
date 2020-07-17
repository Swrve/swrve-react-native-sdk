package com.swrve.reactnative;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

public class SwrvePluginPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        SwrvePluginEventEmitter eventEmitter = new SwrvePluginEventEmitter(reactContext);
        SwrvePluginModule pluginModule = new SwrvePluginModule(reactContext, eventEmitter);
        return Arrays.<NativeModule>asList(pluginModule, eventEmitter);
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
