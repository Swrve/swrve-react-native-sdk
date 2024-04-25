package com.swrve.reactnative

import com.swrve.sdk.ISwrveBase
import com.swrve.sdk.SwrveSDKBase

object SwrveTestUtils {
    @Throws(Exception::class)
    fun setSDKInstance(instance: ISwrveBase<*, *>?) {
        val hack = SwrveSDKBase::class.java.getDeclaredField("instance")
        hack.isAccessible = true
        hack[null] = instance
    }
}
