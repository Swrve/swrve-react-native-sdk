package com.swrve.reactnative

import android.util.Log
import com.swrve.sdk.SwrveLogger
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
abstract class SwrveBaseMockitoTest {

    @Before
    @Throws(Exception::class)
    open fun setUp() {
        SwrveLogger.setLogLevel(Log.VERBOSE)
    }

    @After
    @Throws(Exception::class)
    open fun tearDown() {
        // empty
    }
}
