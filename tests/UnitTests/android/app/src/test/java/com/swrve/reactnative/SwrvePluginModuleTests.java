package com.swrve.reactnative;

import android.app.Activity;

import android.app.NotificationChannel;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.*;
import com.swrve.reactnative.SwrvePluginModule;
import com.swrve.sdk.ISwrve;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveIdentityResponse;
import com.swrve.sdk.SwrvePushNotificationListener;
import com.swrve.sdk.SwrveSilentPushListener;
import com.swrve.sdk.SwrveUserResourcesDiffListener;
import com.swrve.sdk.SwrveUserResourcesListener;
import com.swrve.sdk.SwrveRealTimeUserPropertiesListener;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import static com.swrve.reactnative.SwrvePluginErrorCodes.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.Console;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SwrvePluginModuleTests {

    @Mock
    Swrve swrve;
    @Mock
    Promise promise;
    @Mock
    ReactApplicationContext reactApplicationContext;
    @Mock
    Activity activity;
    @Mock
    SwrvePluginEventEmitter eventEmitter;

    private SwrvePluginModule getModule() {
        // Static, so set to initial state
        SwrvePluginModule.delegateHolder.resetStateForTesting();

        SwrvePluginModule module = new SwrvePluginModule(reactApplicationContext, eventEmitter);
        module.swrveInstance = swrve;
        module.mockedCurrentActivity = activity;

        return module;
    }

    @Test
    public void testStartWithoutUserId() {
        SwrvePluginModule module = getModule();

        // prevents userUpdate from being sent as part of tests
        Mockito.doReturn(false).when(swrve).isStarted();

        module.start(null);

        verify(swrve).start(activity);
    }

    @Test
    public void testStartWithUserId() {
        SwrvePluginModule module = getModule();

        // prevents userUpdate from being sent as part of tests
        Mockito.doReturn(false).when(swrve).isStarted();

        module.start("user-id");

        verify(swrve).start(activity, "user-id");
    }

    @Test
    public void testIdentifyInvalidId() {
        SwrvePluginModule module = getModule();

        module.identify("", promise);

        verify(promise).reject(anyString(), anyString());
    }

    @Test
    public void testIdentifyValidId() {
        SwrvePluginModule module = getModule();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((SwrveIdentityResponse) invocation.getArguments()[1]).onSuccess("great", "swrve-id");
                return null;
            }
        }).when(swrve).identify(anyString(), any(SwrveIdentityResponse.class));

        module.identify("user-id", promise);

        verify(promise).resolve("swrve-id");
    }

    @Test
    public void testIdentifyValidIdSwrveRejection() {
        SwrvePluginModule module = getModule();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((SwrveIdentityResponse) invocation.getArguments()[1]).onError(123, "Error");
                return null;
            }
        }).when(swrve).identify(anyString(), any(SwrveIdentityResponse.class));

        module.identify("user-id", promise);

        verify(promise).reject(swrveResponseCode(123), "Error");
    }

    @Test
    public void testUserUpdate() {
        SwrvePluginModule module = getModule();

        JavaOnlyMap fbMap = new JavaOnlyMap();
        fbMap.putString("string", "string-value");
        fbMap.putBoolean("boolean", true);
        fbMap.putInt("int", 1234);
        fbMap.putDouble("double", 12.45);

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);

        module.userUpdate(fbMap);

        verify(swrve).userUpdate(mapCaptor.capture());
        Map cap = mapCaptor.getValue();

        // The Swrve API only supports Map<String,String> so we convert the non-string
        // values to strings in the plugin
        // This is intended to help dynamic language developers use the API
        assertThat(cap.get("string")).isEqualTo("string-value");
        assertThat(cap.get("boolean")).isEqualTo("true");
        assertThat(cap.get("int")).isEqualTo("1234.0");
        assertThat(cap.get("double")).isEqualTo("12.45");
    }

    @Test
    public void testUserUpdateDate() throws java.text.ParseException {
        SwrvePluginModule module = getModule();
        final String date = "2020-04-17T06:45:56.645Z";
        java.util.TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        df.setTimeZone(tz);
        final Date finalDate = df.parse(date);

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

        module.userUpdateDate("date-key", date);

        verify(swrve).userUpdate(anyString(), dateCaptor.capture());
        Date capturedDate = dateCaptor.getValue();

        assertThat(capturedDate).isEqualTo(finalDate);
    }

    @Test
    public void testEventWithPayload() {
        SwrvePluginModule module = getModule();

        JavaOnlyMap fbMap = new JavaOnlyMap();
        fbMap.putString("string", "string-value");
        fbMap.putBoolean("boolean", true);
        fbMap.putInt("int", 1234);
        fbMap.putDouble("double", 12.45);

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);

        module.event("my-event", fbMap);

        verify(swrve).event(anyString(), mapCaptor.capture());
        Map cap = mapCaptor.getValue();

        // The Swrve API only supports Map<String,String> so we convert the non-string
        // values to strings in the plugin
        // This is intended to help dynamic language developers use the API
        assertThat(cap.get("string")).isEqualTo("string-value");
        assertThat(cap.get("boolean")).isEqualTo("true");
        assertThat(cap.get("int")).isEqualTo("1234.0");
        assertThat(cap.get("double")).isEqualTo("12.45");
    }

    @Test
    public void testSendQueuedEvents() {
        SwrvePluginModule module = getModule();

        module.sendQueuedEvents();

        verify(swrve).sendQueuedEvents();
    }

    @Test
    public void testCurrencyGiven() {
        SwrvePluginModule module = getModule();

        module.currencyGiven("USD", 12);

        verify(swrve).currencyGiven("USD", 12);
    }

    @Test
    public void testPurchase() {
        SwrvePluginModule module = getModule();

        module.purchase("water bottle", "USD", 2, 23);

        verify(swrve).purchase("water bottle", "USD", 23, 2);
    }

    @Test
    public void testUnvalidatedIap() {
        SwrvePluginModule module = getModule();

        module.unvalidatedIap(46.5, "pieces of eight", "DR-3345", 3);

        verify(swrve).iap(3, "DR-3345", 46.5, "pieces of eight");
    }

    // @Test
    public void testUnvalidatedIapWithReward() throws JSONException {
        SwrvePluginModule module = getModule();

        JavaOnlyArray items = new JavaOnlyArray();
        JavaOnlyMap item1 = new JavaOnlyMap();
        JavaOnlyMap item2 = new JavaOnlyMap();

        item1.putString("name", "hoodie");
        item1.putDouble("amount", 13);
        item2.putString("name", "sword");
        item2.putDouble("amount", 145);

        items.pushMap(item1);
        items.pushMap(item2);

        JavaOnlyArray currencies = new JavaOnlyArray();
        JavaOnlyMap curr1 = new JavaOnlyMap();

        curr1.putString("name", "pieces of eight");
        curr1.putDouble("amount", 200);
        currencies.pushMap(curr1);

        JavaOnlyMap fbMap = new JavaOnlyMap();
        fbMap.putArray("items", items);
        fbMap.putArray("currencies", currencies);

        ArgumentCaptor<SwrveIAPRewards> captor = ArgumentCaptor.forClass(SwrveIAPRewards.class);

        module.unvalidatedIapWithReward(46, "pieces of eight", "DR-3345", 3, fbMap);

        verify(swrve).iap(anyInt(), anyString(), anyDouble(), anyString(), captor.capture());

        SwrveIAPRewards swrveRewards = captor.getValue();
        JSONObject rewardJson = swrveRewards.getRewardsJSON();

        assertThat(rewardJson).isNotNull();
        JSONArray jsonItems = rewardJson.getJSONArray("items");
        assertThat(jsonItems).isNotNull();

        assertThat(rewardJson.getJSONArray("items").getJSONObject(0).getString("name")).isEqualTo("name");
        assertThat(rewardJson.getJSONArray("items").getJSONObject(0).getString("amount")).isEqualTo(200);
    }

    @Test
    public void testRefreshCampaignsAndResources() {
        SwrvePluginModule module = getModule();
        module.refreshCampaignsAndResources();
        verify(swrve).refreshCampaignsAndResources();
    }

    @Test
    public void testGetApiKey() {
        SwrvePluginModule module = getModule();
        module.getApiKey(promise);
        verify(swrve).getApiKey();
    }

    @Test
    public void testGetUserId() {
        SwrvePluginModule module = getModule();
        module.getUserId(promise);
        verify(swrve).getUserId();
    }

    @Test
    public void testGetExternalUserId() {
        SwrvePluginModule module = getModule();
        module.getExternalUserId(promise);
        verify(swrve).getExternalUserId();
    }

    @Test
    public void testIsStarted() {
        SwrvePluginModule module = getModule();
        module.isStarted(promise);
        verify(swrve).isStarted();
    }

    @Test
    public void testGetUserResources() {
        SwrvePluginModule module = getModule();
        module.getUserResources(promise);
        verify(swrve).getUserResources(any(SwrveUserResourcesListener.class));
    }

    @Test
    public void testGetUserResourcesDiff() {
        SwrvePluginModule module = getModule();
        module.getUserResourcesDiff(promise);
        verify(swrve).getUserResourcesDiff(any(SwrveUserResourcesDiffListener.class));
    }

    @Test
    public void testGetRealTimeUserProperties() {
        SwrvePluginModule module = getModule();
        module.getRealTimeUserProperties(promise);
        verify(swrve).getRealTimeUserProperties(any(SwrveRealTimeUserPropertiesListener.class));
    }

    @Test
    public void testMessageCenterCampaigns() throws ParseException {
        SwrvePluginModule module = getModule();

        module.getMessageCenterCampaigns(null, promise);
        Mockito.verify(swrve, Mockito.atLeastOnce()).getMessageCenterCampaigns();
    }

    @Test
    public void testMessageCenterCampaign() throws ParseException {
        SwrvePluginModule module = getModule();
        module.getMessageCenterCampaign(1030, null, promise);
        Mockito.verify(swrve, Mockito.atLeastOnce()).getMessageCenterCampaign(1030, null);
    }

    @Test
    public void testShowMessageCenterCampaign() {
        SwrvePluginModule module = getModule();
        // Mock an all the campaign that we use in our Plugin layer.
        SwrveBaseCampaign expectedCampaign = Mockito.mock(SwrveBaseCampaign.class);
        Mockito.doReturn(123).when(expectedCampaign).getId();
        ArrayList<SwrveBaseCampaign> realList = new ArrayList<>();
        realList.add(expectedCampaign);
        Mockito.doReturn(realList).when(swrve).getMessageCenterCampaigns();

        module.showMessageCenterCampaign(123, null);

        Mockito.verify(swrve, Mockito.atLeastOnce()).showMessageCenterCampaign(expectedCampaign);
    }

    @Test
    public void testRemoveMessageCenterCampaign() {
        SwrvePluginModule module = getModule();

        // Mock an all the campaign that we use in our Plugin layer.
        SwrveBaseCampaign expectedCampaign = Mockito.mock(SwrveBaseCampaign.class);
        Mockito.doReturn(122).when(expectedCampaign).getId();
        ArrayList<SwrveBaseCampaign> realList = new ArrayList<>();
        realList.add(expectedCampaign);
        Mockito.doReturn(realList).when(swrve).getMessageCenterCampaigns();

        module.removeMessageCenterCampaign(122);
        Mockito.verify(swrve).removeMessageCenterCampaign(expectedCampaign);
    }

    @Test
    public void testMarkMessageCenterCampaignAsSeen() {
        SwrvePluginModule module = getModule();

        // Mock an all the campaign that we use in our Plugin layer.
        SwrveBaseCampaign expectedCampaign = Mockito.mock(SwrveBaseCampaign.class);
        Mockito.doReturn(133).when(expectedCampaign).getId();
        ArrayList<SwrveBaseCampaign> realList = new ArrayList<>();
        realList.add(expectedCampaign);
        Mockito.doReturn(realList).when(swrve).getMessageCenterCampaigns();

        module.markMessageCenterCampaignAsSeen(133);
        Mockito.verify(swrve).markMessageCenterCampaignAsSeen(expectedCampaign);
    }

    @Test
    public void testPushBufferingWontSendYet() {
        SwrvePluginModule module = getModule();
        SwrvePluginModule.delegateHolder.onPushNotification(new JSONObject());
        SwrvePluginModule.delegateHolder.onSilentPush(reactApplicationContext, new JSONObject());

        verify(eventEmitter, never()).onPushNotification(any(JSONObject.class));
        verify(eventEmitter, never()).onSilentPush(any(Context.class), any(JSONObject.class));
    }

    @Test
    public void testPushBufferingSendsWhenAttached() {
        SwrvePluginModule module = getModule();
        SwrvePluginModule.delegateHolder.onPushNotification(new JSONObject());
        SwrvePluginModule.delegateHolder.onSilentPush(reactApplicationContext, new JSONObject());

        module.startedListening();

        verify(eventEmitter).onPushNotification(any(JSONObject.class));
        verify(eventEmitter).onSilentPush(any(Context.class), any(JSONObject.class));
    }

    @Test
    public void markEmbeddedMessageCampaignAsSeen() {
        String cache = "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}";
        SwrvePluginModule module = getModule();

        Mockito.doReturn(new Date()).when(swrve).getNow();
        Mockito.doReturn(cache).when(swrve).getCachedData(any(), any());
        Mockito.doReturn(new Date()).when(swrve).getInitialisedTime();

        module.markEmbeddedMessageCampaignAsSeen(551899);
        Mockito.verify(swrve).embeddedMessageWasShownToUser(any(SwrveEmbeddedMessage.class));
    }

    @Test
    public void markEmbeddedMessageButtonAsPressed() {
        String cache = "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}";
        SwrvePluginModule module = getModule();

        Mockito.doReturn(new Date()).when(swrve).getNow();
        Mockito.doReturn(cache).when(swrve).getCachedData(any(), any());
        Mockito.doReturn(new Date()).when(swrve).getInitialisedTime();

        module.markEmbeddedMessageButtonAsPressed(551899, "Button 1");
        Mockito.verify(swrve).embeddedMessageButtonWasPressed(any(SwrveEmbeddedMessage.class), eq("Button 1"));
    }

    @Test
    public void testGetPersonalizedText() {
        SwrvePluginModule module = getModule();

        JavaOnlyMap fbMap = new JavaOnlyMap();
        fbMap.putString("test", "value");
        String data = "test data";

        module.getPersonalizedText(data, fbMap, promise);

        verify(swrve).getPersonalizedText(eq(data), anyMap());
    }

    @Test
    public void testGetPersonalizedEmbeddedMessageData() {
        String cache = "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}";
        SwrvePluginModule module = getModule();

        Mockito.doReturn(new Date()).when(swrve).getNow();
        Mockito.doReturn(cache).when(swrve).getCachedData(any(), any());
        Mockito.doReturn(new Date()).when(swrve).getInitialisedTime();

        JavaOnlyMap fbMap = new JavaOnlyMap();
        fbMap.putString("test", "value");

        module.getPersonalizedEmbeddedMessageData(551899, fbMap, promise);

        verify(swrve).getPersonalizedEmbeddedMessageData(any(SwrveEmbeddedMessage.class), anyMap());
    }

    @Test
    public void testStopTracking() {
        SwrvePluginModule module = getModule();

        module.stopTracking();
        verify(swrve).stopTracking();
    }
}
