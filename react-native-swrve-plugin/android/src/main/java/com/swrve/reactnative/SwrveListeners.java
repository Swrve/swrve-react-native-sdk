package com.swrve.reactnative;

import android.content.Context;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushNotificationListener;
import com.swrve.sdk.SwrveResourcesListener;
import com.swrve.sdk.SwrveSilentPushListener;
import com.swrve.sdk.messaging.SwrveEmbeddedListener;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class SwrveListeners implements SwrveSilentPushListener, SwrvePushNotificationListener, SwrveResourcesListener, SwrveEmbeddedListener {

    private static final Object LOCK = new Object();
    private static SwrveListeners instance;

    SwrvePluginEventEmitter pluginEventEmitter = null;
    boolean shouldBufferEvents = true;
    boolean shouldBufferEmbeddedListenerEvents = true;
    List<JSONObject> pushEventBuffer = new LinkedList<>();
    List<JSONObject> silentPushEventBuffer = new LinkedList<>();
    List<EmbeddedListenerHolder> embeddedListenerEventBuffer = new LinkedList<>();

    static SwrveListeners getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new SwrveListeners();
            }
            return instance;
        }
    }

    private SwrveListeners() {
        // empty singleton constructor
    }

    void setEventEmitter(SwrvePluginEventEmitter pluginEventEmitter) {
        this.pluginEventEmitter = pluginEventEmitter;
    }

    void setShouldBufferEvents(boolean shouldBufferEvents) {
        this.shouldBufferEvents = shouldBufferEvents;
    }

    void flushBufferedEvents(Context context) {
        SwrveLogger.d("SwrvePlugin: flushBufferedEvents() push:%s silent:%s", pushEventBuffer.size(), silentPushEventBuffer.size());
        if (pluginEventEmitter == null) {
            return;
        }

        for (JSONObject event : pushEventBuffer) {
            pluginEventEmitter.onPushNotification(event);
        }
        pushEventBuffer.clear();

        for (JSONObject event : silentPushEventBuffer) {
            pluginEventEmitter.onSilentPush(context, event);
        }
        silentPushEventBuffer.clear();
    }

    void setShouldBufferEmbeddedListenerEvents(boolean shouldBufferEmbeddedListenerEvents) {
        this.shouldBufferEmbeddedListenerEvents = shouldBufferEmbeddedListenerEvents;
    }

    void flushBufferedEmbeddedListenerEvents() {
        SwrveLogger.d("SwrvePlugin: flushBufferedEmbeddedEvents() embedded:%s", embeddedListenerEventBuffer.size());
        if (pluginEventEmitter == null) {
            return;
        }
        for (EmbeddedListenerHolder holder : embeddedListenerEventBuffer) {
            pluginEventEmitter.onEmbeddedCallback(holder.message, holder.personalizationProperties, holder.isControl);
        }
        embeddedListenerEventBuffer.clear();
    }

    @Override
    public void onPushNotification(JSONObject payload) {
        if (pluginEventEmitter != null && !shouldBufferEvents) {
            SwrveLogger.d("SwrvePlugin: onPushNotification() passing through");
            pluginEventEmitter.onPushNotification(payload);
        } else {
            SwrveLogger.d("SwrvePlugin: onPushNotification() buffered");
            pushEventBuffer.add(payload);
        }
    }

    @Override
    public void onSilentPush(Context context, JSONObject payload) {
        if (pluginEventEmitter != null && !shouldBufferEvents) {
            SwrveLogger.d("SwrvePlugin: onSilentPush() passing through");
            pluginEventEmitter.onSilentPush(context, payload);
        } else {
            SwrveLogger.d("SwrvePlugin: onSilentPush() buffered");
            silentPushEventBuffer.add(payload);
        }
    }

    @Override
    public void onResourcesUpdated() {
        if (pluginEventEmitter != null) {
            SwrveLogger.d("SwrvePlugin: userResources()");
            pluginEventEmitter.onResourcesUpdated();
        }
    }

    @Override
    public void onMessage(Context context, SwrveEmbeddedMessage message, Map<String, String> personalizationProperties, boolean isControl) {
        SwrveLogger.d("SwrvePlugin: SwrveEmbeddedListener.onMessage()");
        if (pluginEventEmitter != null && !shouldBufferEmbeddedListenerEvents) {
            SwrveLogger.d("SwrvePlugin: SwrveEmbeddedListener.onMessage() passing through - not buffered");
            pluginEventEmitter.onEmbeddedCallback(message, personalizationProperties, isControl);
        } else {
            SwrveLogger.w("SwrvePlugin: SwrveEmbeddedListener.onMessage() was triggered but need to buffer until receiver is ready");
            EmbeddedListenerHolder holder = new EmbeddedListenerHolder(message, personalizationProperties, isControl);
            embeddedListenerEventBuffer.add(holder);
        }
    }

    class EmbeddedListenerHolder {
        final SwrveEmbeddedMessage message;
        final Map<String, String> personalizationProperties;
        final boolean isControl;

        EmbeddedListenerHolder(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties, boolean isControl) {
            this.message = message;
            this.personalizationProperties = personalizationProperties;
            this.isControl = isControl;
        }
    }
}
