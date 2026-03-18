package com.swrve.reactnative

import android.app.Activity
import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.soloader.SoLoader
import com.google.common.truth.Truth
import com.swrve.sdk.Swrve
import com.swrve.sdk.SwrveIAPRewards
import com.swrve.sdk.SwrveIdentityResponse
import com.swrve.sdk.SwrveRealTimeUserPropertiesListener
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.SwrveRefreshContentListener
import com.swrve.sdk.SwrveRefreshContentListenerResult
import com.swrve.sdk.SwrveUserResourcesDiffListener
import com.swrve.sdk.SwrveUserResourcesListener
import com.swrve.sdk.messaging.SwrveBaseCampaign
import com.swrve.sdk.messaging.SwrveEmbeddedMessage
import com.swrve.sdk.messaging.SwrveMessageButtonDetails
import com.swrve.sdk.messaging.SwrveMessageDetails
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SwrvePluginModuleTest : SwrveBaseMockitoTest() {

    @Mock
    var mockSwrve: Swrve? = null

    @Mock
    var mockReactApplicationContext: ReactApplicationContext? = null

    @Mock
    var mockPromise: Promise? = null

    @Mock
    var mockeEventEmitter: SwrvePluginEventEmitter? = null

    @Mock
    var mockActivity: Activity? = null

    var spyPluginModule: SwrvePluginModule? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        SoLoader.init(mockReactApplicationContext?.applicationContext, false)
        spyPluginModule = spy(SwrvePluginModule(mockReactApplicationContext, mockeEventEmitter!!))
        doReturn(mockActivity).`when`(mockReactApplicationContext)!!.currentActivity
        SwrveTestUtils.setSDKInstance(mockSwrve)
    }

    @Test
    fun testStartWithoutUserId() {
        spyPluginModule?.start(null)
        verify<Swrve>(mockSwrve).start(mockActivity)
    }

    @Test
    fun testStartWithUserId() {
        spyPluginModule?.start("user-id")
        verify<Swrve>(mockSwrve).start(mockActivity, "user-id")
    }

    @Test
    fun testSendQueuedEvents() {
        spyPluginModule?.sendQueuedEvents()
        verify<Swrve>(mockSwrve).sendQueuedEvents()
    }

    @Test
    fun testIdentifyInvalidId() {
        spyPluginModule?.identify("", mockPromise)
        verify(mockPromise)?.reject(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

    @Test
    fun testIdentifyValidId() {
        doAnswer { invocation ->
            (invocation.arguments[1] as SwrveIdentityResponse).onSuccess("great", "swrve-id")
            null
        }.`when`<Swrve>(mockSwrve).identify(ArgumentMatchers.anyString(), any<SwrveIdentityResponse>(SwrveIdentityResponse::class.java))
        spyPluginModule?.identify("user-id", mockPromise)
        verify(mockPromise)?.resolve("swrve-id")
    }

    @Test
    fun testIdentifyValidIdSwrveRejection() {

        doAnswer { invocation ->
            (invocation.arguments[1] as SwrveIdentityResponse).onError(123, "Error")
            null
        }.`when`<Swrve>(mockSwrve).identify(ArgumentMatchers.anyString(), any(SwrveIdentityResponse::class.java))
        spyPluginModule?.identify("user-id", mockPromise)
        verify<Promise>(mockPromise).reject(SwrvePluginErrorCodes.swrveResponseCode(123), "Error")
    }

    @Test
    fun testUserUpdate() {
        val fbMap = JavaOnlyMap()
        fbMap.putString("string", "string-value")
        fbMap.putBoolean("boolean", true)
        fbMap.putInt("int", 1234)
        fbMap.putDouble("double", 12.45)
        val mapCaptor = ArgumentCaptor.forClass(Map::class.java)
        spyPluginModule?.userUpdate(fbMap)
        verify<Swrve>(mockSwrve).userUpdate(mapCaptor.capture() as MutableMap<String, String>?)
        val cap = mapCaptor.value

        Truth.assertThat(cap["string"]).isEqualTo("string-value")
        Truth.assertThat(cap["boolean"]).isEqualTo("true")
        Truth.assertThat(cap["int"]).isEqualTo("1234.0")
        Truth.assertThat(cap["double"]).isEqualTo("12.45")
    }

    @Test
    @Throws(ParseException::class)
    fun testUserUpdateDate() {
        val date = "2020-04-17T06:45:56.645Z"
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        df.timeZone = tz
        val finalDate = df.parse(date)
        val dateCaptor = ArgumentCaptor.forClass(Date::class.java)
        spyPluginModule?.userUpdateDate("date-key", date)
        verify<Swrve>(mockSwrve).userUpdate(ArgumentMatchers.anyString(), dateCaptor.capture())
        val capturedDate = dateCaptor.value
        Truth.assertThat(capturedDate).isEqualTo(finalDate)
    }

    @Test
    fun testEventWithPayload() {
        val fbMap = JavaOnlyMap()
        fbMap.putString("string", "string-value")
        fbMap.putBoolean("boolean", true)
        fbMap.putInt("int", 1234)
        fbMap.putDouble("double", 12.45)
        val mapCaptor = ArgumentCaptor.forClass(Map::class.java)
        spyPluginModule?.event("my-event", fbMap)
        verify<Swrve>(mockSwrve).event(ArgumentMatchers.anyString(), mapCaptor.capture() as MutableMap<String, String>?)
        val cap = mapCaptor.value

        Truth.assertThat(cap["string"]).isEqualTo("string-value")
        Truth.assertThat(cap["boolean"]).isEqualTo("true")
        Truth.assertThat(cap["int"]).isEqualTo("1234.0")
        Truth.assertThat(cap["double"]).isEqualTo("12.45")
    }

    @Test
    fun testCurrencyGiven() {
        spyPluginModule?.currencyGiven("USD", 12)
        verify<Swrve>(mockSwrve).currencyGiven("USD", 12.0)
    }

    @Test
    fun testPurchase() {
        spyPluginModule?.purchase("water bottle", "USD", 2, 23)
        verify<Swrve>(mockSwrve).purchase("water bottle", "USD", 23, 2)
    }

    @Test
    fun testUnvalidatedIap() {
        spyPluginModule?.unvalidatedIap(46.5, "pieces of eight", "DR-3345", 3)
        verify<Swrve>(mockSwrve).iap(3, "DR-3345", 46.5, "pieces of eight")
    }

//    @Test
    @Throws(JSONException::class)
    fun testUnvalidatedIapWithReward() {
        val items = JavaOnlyArray()
        val item1 = JavaOnlyMap()
        val item2 = JavaOnlyMap()
        item1.putString("name", "hoodie")
        item1.putDouble("amount", 13.0)
        item2.putString("name", "sword")
        item2.putDouble("amount", 145.0)
        items.pushMap(item1)
        items.pushMap(item2)
        val currencies = JavaOnlyArray()
        val curr1 = JavaOnlyMap()
        curr1.putString("name", "pieces of eight")
        curr1.putDouble("amount", 200.0)
        currencies.pushMap(curr1)
        val fbMap = JavaOnlyMap()
        fbMap.putArray("items", items)
        fbMap.putArray("currencies", currencies)
        val captor = ArgumentCaptor.forClass(SwrveIAPRewards::class.java)
        spyPluginModule?.unvalidatedIapWithReward(46.0, "pieces of eight", "DR-3345", 3, fbMap)
        verify<Swrve>(mockSwrve).iap(ArgumentMatchers.anyInt(), ArgumentMatchers.anyString(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyString(), captor.capture())
        val swrveRewards = captor.value
        val rewardJson = swrveRewards.rewardsJSON
        Truth.assertThat(rewardJson).isNotNull()
        val jsonItems = rewardJson.getJSONArray("items")
        Truth.assertThat(jsonItems).isNotNull()
        Truth.assertThat(rewardJson.getJSONArray("items").getJSONObject(0).getString("name")).isEqualTo("name")
        Truth.assertThat(rewardJson.getJSONArray("items").getJSONObject(0).getString("amount")).isEqualTo(200)
    }

    @Test
    fun testRefreshCampaignsAndResources() {
        spyPluginModule?.refreshCampaignsAndResources()
        verify<Swrve>(mockSwrve).refreshContent(null)
    }

    @Test
    fun testRefreshContentSuccess() {
        mockStatic(Arguments::class.java).use { mockedArguments ->
            // Mock Arguments.createMap() to return JavaOnlyMap
            mockedArguments.`when`<JavaOnlyMap> { Arguments.createMap() }.thenAnswer { JavaOnlyMap() }

            // Mock the SwrveSDK.refreshContent to capture listener and invoke it with success
            doAnswer { invocation ->
                val listener = invocation.getArgument<SwrveRefreshContentListener>(0)
                val result = SwrveRefreshContentListenerResult(
                    SwrveRefreshContentListenerResult.ResultCode.SUCCESS,
                    "",
                    200
                )
                listener.onComplete(result)
                null
            }.`when`(mockSwrve)!!.refreshContent(any())

            spyPluginModule?.refreshContent(mockPromise)

            // Verify that the listener was passed to the native SDK
            verify<Swrve>(mockSwrve).refreshContent(any())

            // Capture the argument passed to promise.resolve
            val captor = ArgumentCaptor.forClass(Any::class.java)
            verify(mockPromise)?.resolve(captor.capture())

            // Verify the result map contains correct values
            val resultMap = captor.value as com.facebook.react.bridge.WritableMap
            Truth.assertThat(resultMap.getString("resultCode")).isEqualTo("SUCCESS")
            Truth.assertThat(resultMap.getString("errorMessage")).isEqualTo("")
            Truth.assertThat(resultMap.getInt("httpResponseCode")).isEqualTo(200)
        }
    }

    @Test
    fun testRefreshContentError() {
        mockStatic(Arguments::class.java).use { mockedArguments ->
            // Mock Arguments.createMap() to return JavaOnlyMap
            mockedArguments.`when`<JavaOnlyMap> { Arguments.createMap() }.thenAnswer { JavaOnlyMap() }

            // Mock the SwrveSDK.refreshContent to capture listener and invoke it with error
            doAnswer { invocation ->
                val listener = invocation.getArgument<SwrveRefreshContentListener>(0)
                val result = SwrveRefreshContentListenerResult(
                    SwrveRefreshContentListenerResult.ResultCode.ERROR,
                    "Network error",
                    500
                )
                listener.onComplete(result)
                null
            }.`when`(mockSwrve)!!.refreshContent(any())

            spyPluginModule?.refreshContent(mockPromise)

            // Verify that the listener was passed to the native SDK
            verify<Swrve>(mockSwrve).refreshContent(any())

            // Capture the argument passed to promise.resolve
            val captor = ArgumentCaptor.forClass(Any::class.java)
            verify(mockPromise)?.resolve(captor.capture())

            // Verify the result map contains correct error values
            val resultMap = captor.value as com.facebook.react.bridge.WritableMap
            Truth.assertThat(resultMap.getString("resultCode")).isEqualTo("ERROR")
            Truth.assertThat(resultMap.getString("errorMessage")).isEqualTo("Network error")
            Truth.assertThat(resultMap.getInt("httpResponseCode")).isEqualTo(500)
        }
    }

    @Test
    fun testRefreshContentErrorUnknown() {
        mockStatic(Arguments::class.java).use { mockedArguments ->
            // Mock Arguments.createMap() to return JavaOnlyMap
            mockedArguments.`when`<JavaOnlyMap> { Arguments.createMap() }.thenAnswer { JavaOnlyMap() }

            // Mock the SwrveSDK.refreshContent to capture listener and invoke it with unknown error
            doAnswer { invocation ->
                val listener = invocation.getArgument<SwrveRefreshContentListener>(0)
                val result = SwrveRefreshContentListenerResult(
                    SwrveRefreshContentListenerResult.ResultCode.ERROR_UNKNOWN,
                    "Unknown error occurred",
                    0
                )
                listener.onComplete(result)
                null
            }.`when`(mockSwrve)!!.refreshContent(any())

            spyPluginModule?.refreshContent(mockPromise)

            // Verify that the listener was passed to the native SDK
            verify<Swrve>(mockSwrve).refreshContent(any())

            // Capture the argument passed to promise.resolve
            val captor = ArgumentCaptor.forClass(Any::class.java)
            verify(mockPromise)?.resolve(captor.capture())

            // Verify the result map contains correct error values
            val resultMap = captor.value as com.facebook.react.bridge.WritableMap
            Truth.assertThat(resultMap.getString("resultCode")).isEqualTo("ERROR_UNKNOWN")
            Truth.assertThat(resultMap.getString("errorMessage")).isEqualTo("Unknown error occurred")
            Truth.assertThat(resultMap.getInt("httpResponseCode")).isEqualTo(0)
        }
    }

    @Test
    fun testRefreshContentException() {
        // Mock the SwrveSDK.refreshContent to throw an exception
        doAnswer {
            throw RuntimeException("Test exception")
        }.`when`(mockSwrve)!!.refreshContent(any())

        spyPluginModule?.refreshContent(mockPromise)

        // Verify that the promise was rejected with the exception
        verify(mockPromise)?.reject(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

    @Test
    fun testGetApiKey() {
        spyPluginModule?.getApiKey(mockPromise)
        verify<Swrve>(mockSwrve).apiKey
    }

    @Test
    fun testGetUserId() {
        spyPluginModule?.getUserId(mockPromise)
        verify<Swrve>(mockSwrve).userId
    }

    @Test
    fun testGetExternalUserId() {
        spyPluginModule?.getExternalUserId(mockPromise)
        verify<Swrve>(mockSwrve).externalUserId
    }

    @Test
    fun testIsStarted() {
        spyPluginModule?.isStarted(mockPromise)
        verify<Swrve>(mockSwrve).isStarted
    }

    @Test
    fun testGetUserResources() {
        spyPluginModule?.getUserResources(mockPromise)
        verify<Swrve>(mockSwrve).getUserResources(any(SwrveUserResourcesListener::class.java))
    }

    @Test
    fun testGetUserResourcesDiff() {
        spyPluginModule?.getUserResourcesDiff(mockPromise)
        verify<Swrve>(mockSwrve).getUserResourcesDiff(any(SwrveUserResourcesDiffListener::class.java))
    }

    @Test
    fun testGetRealTimeUserProperties() {
        spyPluginModule?.getRealTimeUserProperties(mockPromise)
        verify<Swrve>(mockSwrve).getRealTimeUserProperties(any(SwrveRealTimeUserPropertiesListener::class.java))
    }

    @Test
    @Throws(ParseException::class)
    fun testMessageCenterCampaigns() {
        spyPluginModule?.getMessageCenterCampaigns(null, mockPromise)
        verify<Swrve>(mockSwrve, atLeastOnce()).messageCenterCampaigns
    }

    @Test
    @Throws(ParseException::class)
    fun testEmbeddedMessageCenterCampaigns_interactionOnly() {
        try {
            spyPluginModule?.getEmbeddedMessageCenterCampaigns(null, mockPromise)
        } catch (e: NoClassDefFoundError) {
            // RN WritableNativeArray requires native libs; not available in unit tests
        }
        // Verify the SDK method was invoked with null personalization
        verify<Swrve>(mockSwrve, atLeastOnce()).getEmbeddedMessageCenterCampaigns(null)
    }

    @Test
    @Throws(ParseException::class)
    fun testMessageCenterCampaign() {
        spyPluginModule?.getMessageCenterCampaign(1030, null, mockPromise)
        verify<Swrve>(mockSwrve, atLeastOnce()).getMessageCenterCampaign(1030, null)
    }

    @Test
    fun testShowMessageCenterCampaign() {
        // Mock an all the campaign that we use in our Plugin layer.
        val expectedCampaign = mock(SwrveBaseCampaign::class.java)
        doReturn(123).`when`(expectedCampaign).id
        val realList = ArrayList<SwrveBaseCampaign>()
        realList.add(expectedCampaign)
        doReturn(realList).`when`<Swrve>(mockSwrve).messageCenterCampaigns
        spyPluginModule?.showMessageCenterCampaign(123, null)
        verify<Swrve>(mockSwrve, atLeastOnce()).showMessageCenterCampaign(expectedCampaign)
    }

    @Test
    fun testRemoveMessageCenterCampaign() {
        // Mock an all the campaign that we use in our Plugin layer.
        val expectedCampaign = mock(SwrveBaseCampaign::class.java)
        doReturn(122).`when`(expectedCampaign).id
        val realList = ArrayList<SwrveBaseCampaign>()
        realList.add(expectedCampaign)
        doReturn(realList).`when`<Swrve>(mockSwrve).messageCenterCampaigns
        spyPluginModule?.removeMessageCenterCampaign(122)
        verify<Swrve>(mockSwrve).removeMessageCenterCampaign(expectedCampaign)
    }

    @Test
    fun testMarkMessageCenterCampaignAsSeen() {
        // Mock an all the campaign that we use in our Plugin layer.
        val expectedCampaign = mock(SwrveBaseCampaign::class.java)
        doReturn(133).`when`(expectedCampaign).id
        val realList = ArrayList<SwrveBaseCampaign>()
        realList.add(expectedCampaign)
        doReturn(realList).`when`<Swrve>(mockSwrve).messageCenterCampaigns
        spyPluginModule?.markMessageCenterCampaignAsSeen(133)
        verify<Swrve>(mockSwrve).markMessageCenterCampaignAsSeen(expectedCampaign)
    }

    @Test
    fun testEmbeddedControlMessageImpressionSent() {
        val cache = "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}"
        doReturn(Date()).`when`<Swrve>(mockSwrve).now
        doReturn(cache).`when`<Swrve>(mockSwrve).getCachedData(any(), any())
        doReturn(Date()).`when`<Swrve>(mockSwrve).initialisedTime
        spyPluginModule?.embeddedControlMessageImpressionEvent(551899)
        verify<Swrve>(mockSwrve).embeddedControlMessageImpressionEvent(any(SwrveEmbeddedMessage::class.java))
    }

    @Test
    fun testPushBufferingWontSendYet() {
        SwrveListeners.getInstance().shouldBufferEvents = true
        SwrveListeners.getInstance().onPushNotification(JSONObject())
        SwrveListeners.getInstance().onSilentPush(mockReactApplicationContext, JSONObject())
        verify(mockeEventEmitter, never())?.onPushNotification(any(JSONObject::class.java))
        verify(mockeEventEmitter, never())?.onSilentPush(any(Context::class.java), any(JSONObject::class.java))
    }

    @Test
    fun testPushBufferingSendsWhenAttached() {
        SwrveListeners.getInstance().onPushNotification(JSONObject())
        SwrveListeners.getInstance().onSilentPush(mockReactApplicationContext, JSONObject())
        spyPluginModule?.startedListening()
        verify(mockeEventEmitter)?.onPushNotification(any(JSONObject::class.java))
        verify(mockeEventEmitter)?.onSilentPush(any(Context::class.java), any(JSONObject::class.java))
    }

    @Test
    fun testEmbeddedBufferingWontSendYet() {
        assertEquals(true, SwrveListeners.getInstance().shouldBufferEmbeddedListenerEvents)
        SwrveListeners.getInstance().onMessage(mockReactApplicationContext, mock(SwrveEmbeddedMessage::class.java), HashMap(), true)
        verify(mockeEventEmitter, never())?.onEmbeddedCallback(any(SwrveEmbeddedMessage::class.java), anyMap(), anyBoolean())
    }

    @Test
    fun testEmbeddedBufferingSendsWhenAttached() {
        assertEquals(true, SwrveListeners.getInstance().shouldBufferEmbeddedListenerEvents)
        val mockSwrveMessage = mock(SwrveEmbeddedMessage::class.java);
        val hashMap = HashMap<String, String>()
        SwrveListeners.getInstance().onMessage(mockReactApplicationContext, mockSwrveMessage, hashMap, true)
        spyPluginModule?.startedEmbeddedListening()
        verify(mockeEventEmitter)?.onEmbeddedCallback(mockSwrveMessage, hashMap, true)
        assertEquals(false, SwrveListeners.getInstance().shouldBufferEmbeddedListenerEvents)
    }

    @Test
    fun markEmbeddedMessageCampaignAsSeen() {
        val cache = "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}"
        doReturn(Date()).`when`<Swrve>(mockSwrve).now
        doReturn(cache).`when`<Swrve>(mockSwrve).getCachedData(any<String>(), any<String>())
        doReturn(Date()).`when`<Swrve>(mockSwrve).initialisedTime
        val swrve = SwrveSDK.getInstance() as Swrve
        spyPluginModule?.markEmbeddedMessageCampaignAsSeen(551899)
        verify<Swrve>(mockSwrve).embeddedMessageWasShownToUser(any(SwrveEmbeddedMessage::class.java))
    }

    @Test
    fun markEmbeddedMessageButtonAsPressed() {
        val cache =
            "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}"
        doReturn(Date()).`when`<Swrve>(mockSwrve).now
        doReturn(cache).`when`<Swrve>(mockSwrve).getCachedData(any(), any())
        doReturn(Date()).`when`<Swrve>(mockSwrve).initialisedTime
        spyPluginModule?.markEmbeddedMessageButtonAsPressed(551899, "Button 1")
        verify<Swrve>(mockSwrve).embeddedMessageButtonWasPressed(any(SwrveEmbeddedMessage::class.java), eq("Button 1"))
    }

    @Test
    fun testGetPersonalizedText() {
        val fbMap = JavaOnlyMap()
        fbMap.putString("test", "value")
        val data = "test data"
        spyPluginModule?.getPersonalizedText(data, fbMap, mockPromise)
        verify<Swrve>(mockSwrve).getPersonalizedText(eq(data), anyMap())
    }

    @Test
    fun testGetPersonalizedEmbeddedMessageData() {
        val cache = "{\"campaigns\": [{\"id\":551899,\"start_date\":1630510173000,\"end_date\":2145920400000,\"rules\":{\"delay_first_message\":180,\"dismiss_after_views\":99999,\"display_order\":\"random\",\"min_delay_between_messages\":60},\"message_center\":true,\"embedded_message\":{\"id\":547716,\"name\":\"Test Embedded Campaign\",\"data\":\"Hello this is a rendered embedded message.\",\"type\":\"other\",\"buttons\":[],\"version\":1,\"rules\":{},\"priority\":9999},\"subject\":\"\"}]}"
        doReturn(Date()).`when`<Swrve>(mockSwrve).now
        doReturn(cache).`when`<Swrve>(mockSwrve).getCachedData(any<String>(), any<String>())
        doReturn(Date()).`when`<Swrve>(mockSwrve).initialisedTime
        val fbMap = JavaOnlyMap()
        fbMap.putString("test", "value")
        spyPluginModule?.getPersonalizedEmbeddedMessageData(551899, fbMap, mockPromise)
        verify<Swrve>(mockSwrve).getPersonalizedEmbeddedMessageData(any<SwrveEmbeddedMessage>(SwrveEmbeddedMessage::class.java), anyMap<String, String>())
    }

    @Test
    fun testStopTracking() {
        spyPluginModule?.stopTracking()
        verify<Swrve>(mockSwrve).stopTracking()
    }

    @Test
    fun testInAppMessageMappingReturnsValidJSONWhenObjectHasNilValues() {
        // Given
        val eventEmitter = SwrvePluginEventEmitter(mockReactApplicationContext)
        val messageDetails = mock(SwrveMessageDetails::class.java)
        val longId: Long = 551899
        doReturn(null).`when`(messageDetails).campaignSubject
        doReturn(longId).`when`(messageDetails).campaignId
        doReturn(longId).`when`(messageDetails).variantId
        doReturn(null).`when`(messageDetails).messageName
        val buttonDetails = mock(SwrveMessageButtonDetails::class.java)
        doReturn(null).`when`(buttonDetails).buttonName
        val list = java.util.ArrayList<SwrveMessageButtonDetails>()
        list.add(buttonDetails)
        doReturn(list).`when`(messageDetails).buttons
        val json = eventEmitter.messageDetailsToJson(messageDetails)
        Truth.assertThat(json).isNotNull()
        assertEquals("", json["campaignSubject"])
        assertEquals("", json["messageName"])
        Truth.assertThat(json.getJSONArray("buttons").getJSONObject(0).getString("buttonName")).isEqualTo("")
    }

    @Test
    fun testInAppMessageMappingReturnsValidValues() {
        // Given
        val eventEmitter = SwrvePluginEventEmitter(mockReactApplicationContext)
        val messageDetails = mock(SwrveMessageDetails::class.java)
        val longId: Long = 551899
        doReturn("campaignSubject").`when`(messageDetails).campaignSubject
        doReturn(longId).`when`(messageDetails).campaignId
        doReturn(longId).`when`(messageDetails).variantId
        doReturn("messageName").`when`(messageDetails).messageName
        val buttonDetails = mock(SwrveMessageButtonDetails::class.java)
        doReturn("buttonName").`when`(buttonDetails).buttonName
        val list = java.util.ArrayList<SwrveMessageButtonDetails>()
        list.add(buttonDetails)
        doReturn(list).`when`(messageDetails).buttons
        val json = eventEmitter.messageDetailsToJson(messageDetails)
        Truth.assertThat(json).isNotNull()
        assertEquals("campaignSubject", json["campaignSubject"])
        assertEquals("messageName", json["messageName"])
        Truth.assertThat(json.getJSONArray("buttons").getJSONObject(0).getString("buttonName")).isEqualTo("buttonName")
    }

    @Test
    fun testPushInboxGetMessages() {
        spyPluginModule?.getPushInboxMessages(mockPromise)
        verify<Swrve>(mockSwrve, atLeastOnce()).pushInboxMessages
    }

    @Test
    fun testPushInboxMessageEngage() {
        val messageId = 3614565938492356600
        spyPluginModule?.engagePushInboxMessage(messageId.toString(), mockPromise)
        verify<Swrve>(mockSwrve, atLeastOnce()).engagePushInboxMessage(eq(messageId), any())
    }

    @Test
    fun testPushInboxMessageRead() {
        val messageId = 3614565938492356600
        spyPluginModule?.readPushInboxMessage(messageId.toString(), mockPromise)
        verify<Swrve>(mockSwrve, atLeastOnce()).readPushInboxMessage(eq(messageId), any())
    }

    @Test
    fun testPushInboxMessageDelete() {
        val messageId = 3614565938492356600
        spyPluginModule?.deletePushInboxMessage(messageId.toString(), mockPromise)
        verify<Swrve>(mockSwrve, atLeastOnce()).deletePushInboxMessage(eq(messageId), any())
    }
}
