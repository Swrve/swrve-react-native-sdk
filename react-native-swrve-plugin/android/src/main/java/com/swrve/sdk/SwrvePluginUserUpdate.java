package com.swrve.sdk;

import static com.swrve.reactnative.SwrvePluginModule.SWRVE_PLUGIN_VERSION;

import android.content.Context;

import com.facebook.react.modules.systeminfo.ReactNativeVersion;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SwrvePluginUserUpdate {

    final Context context;

    public SwrvePluginUserUpdate(Context context) {
        this.context = context;
        if (SwrveSDK.getInstance() != null && SwrveSDK.getInstance() instanceof ISwrveCommon) {
            ISwrveCommon swrveCommon = (ISwrveCommon) SwrveSDK.getInstance();
            swrveCommon.setSessionListener(this::sendSwrveUserUpdate);
        }
    }

    void sendSwrveUserUpdate() {
        Map<String, String> userUpdates = new HashMap<>();
        userUpdates.put("swrve.react_native_plugin_version", SWRVE_PLUGIN_VERSION);
        userUpdates.put("swrve.react_native_version", getReactNativeVersion());
        userUpdates.put("swrve.react_native_new_arch_enabled", isNewArchitectureEnabled() ? "true" : "false");
        SwrveSDK.userUpdate(userUpdates);
    }

    private String getReactNativeVersion() {
        StringBuilder semanticVersion = new StringBuilder();
        try {
            Map<String, Object> reactNativeVersionMap = ReactNativeVersion.VERSION;
            if (!reactNativeVersionMap.containsKey("major") ||
                    !reactNativeVersionMap.containsKey("minor") ||
                    !reactNativeVersionMap.containsKey("patch")) {
                return semanticVersion.toString();
            }

            Object major = reactNativeVersionMap.get("major");
            Object minor = reactNativeVersionMap.get("minor");
            Object patch = reactNativeVersionMap.get("patch");
            semanticVersion.append(major).append('.').append(minor).append('.').append(patch);

            Object prerelease = reactNativeVersionMap.get("prerelease");
            if (prerelease != null) {
                semanticVersion.append('-').append(prerelease);
            }
        } catch (Exception e) {
            SwrveLogger.w("SwrvePlugin: Unable to get react native semantic version. Setting as Unknown. %s", e.getMessage());
            semanticVersion = new StringBuilder("Unknown");
        }
        return semanticVersion.toString();
    }

    protected boolean isNewArchitectureEnabled() {
        boolean isNewArchitectureEnabled = false;
        try {
            // try to safely access it through reflection
            Class<?> buildConfigClass = Class.forName(context.getPackageName() + ".BuildConfig");
            Field field = buildConfigClass.getField("IS_NEW_ARCHITECTURE_ENABLED");
            isNewArchitectureEnabled = field.getBoolean(null);
        } catch (Exception e) {
            // Ignore exception
        }
        return isNewArchitectureEnabled;
    }
}
