package com.swrve.sdk

import android.content.Context
import com.swrve.reactnative.SwrveBaseMockitoTest
import com.swrve.reactnative.SwrveTestUtils
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class SwrvePluginUtilsTest : SwrveBaseMockitoTest() {

    @Mock
    var mockSwrve: Swrve? = null

    @Mock
    var mockContext: Context? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        SwrveTestUtils.setSDKInstance(mockSwrve)
    }

    @Test
    fun testSetSessionListener() {
        SwrvePluginUserUpdate(mockContext)
        verify<Swrve>(mockSwrve).setSessionListener(any())
    }

    @Test
    fun testSendSwrveUserUpdate() {
        doReturn("com.unittests").`when`(mockContext)?.packageName
        SwrvePluginUserUpdate(mockContext).sendSwrveUserUpdate()
        val map = HashMap<String, String>()
        map["swrve.react_native_version"] = "0.73.4"
        map["swrve.react_native_plugin_version"] = "5.0.0"
        map["swrve.react_native_new_arch_enabled"] = "false"
        verify<Swrve>(mockSwrve).userUpdate(map)

        // Test with new architecture enabled by mocking is new architecture enabled
        val spySwrvePluginUtils = spy(SwrvePluginUserUpdate(mockContext))
        doReturn(true).`when`(spySwrvePluginUtils)?.isNewArchitectureEnabled()
        spySwrvePluginUtils.sendSwrveUserUpdate()
        val map2 = HashMap<String, String>()
        map2["swrve.react_native_version"] = "0.73.4"
        map2["swrve.react_native_plugin_version"] = "5.0.0"
        map2["swrve.react_native_new_arch_enabled"] = "true"
        verify<Swrve>(mockSwrve).userUpdate(map2)
    }
}