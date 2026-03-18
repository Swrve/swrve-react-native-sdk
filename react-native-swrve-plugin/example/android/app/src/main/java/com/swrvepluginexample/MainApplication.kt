package com.swrvepluginexample

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.swrve.reactnative.SwrvePlugin
import com.swrve.sdk.SwrveNotificationConfig
import com.swrve.sdk.config.SwrveConfig

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()

    val config = SwrveConfig()
    val notificationConfig = getNotificationConfig()
    config.setNotificationConfig(notificationConfig)

    val YOUR_APP_ID = 123
    val YOUR_API_KEY = "YOUR_API_KEY"
    SwrvePlugin.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config)

    loadReactNative(this)
  }

  private fun getNotificationConfig(): SwrveNotificationConfig {
    var channel: NotificationChannel? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      channel = NotificationChannel("123", "Swrve default channel", NotificationManager.IMPORTANCE_DEFAULT)
      if (getSystemService(NOTIFICATION_SERVICE) != null) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
      }
    }
    val notificationEvents = listOf("subscribe_push")
    return SwrveNotificationConfig.Builder(R.drawable.ic_notification, channel)
      .pushNotificationPermissionEvents(notificationEvents)
      .build()
  }

}
