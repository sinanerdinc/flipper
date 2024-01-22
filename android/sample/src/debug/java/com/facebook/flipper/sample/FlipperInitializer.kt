/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.flipper.sample

import android.app.Application
import android.content.Context
import com.facebook.flipper.core.FlipperClient
import com.facebook.flipper.plugins.crashreporter.CrashReporterPlugin
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.example.ExampleFlipperPlugin
import com.facebook.flipper.plugins.fresco.FrescoFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.jetpackcompose.UIDebuggerComposeSupport
import com.facebook.flipper.plugins.navigation.NavigationFlipperPlugin
import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin.SharedPreferencesDescriptor
import com.facebook.flipper.plugins.uidebugger.UIDebuggerFlipperPlugin
import com.facebook.flipper.plugins.uidebugger.core.UIDContext
import com.facebook.flipper.plugins.uidebugger.descriptors.DescriptorRegister
import com.facebook.flipper.plugins.uidebugger.litho.UIDebuggerLithoSupport
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.editor.flipper.LithoFlipperDescriptors
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder

object FlipperInitializer {

  @JvmStatic
  fun initFlipperPlugins(context: Context?, client: FlipperClient): InitializationResult {
    val descriptorMapping = DescriptorMapping.withDefaults()
    val networkPlugin = NetworkFlipperPlugin()
    val interceptor = FlipperOkhttpInterceptor(networkPlugin, true)

    // Normally, you would want to make this dependent on a BuildConfig flag, but
    // for this demo application we can safely assume that you always want to debug.
    // TODO: T174494880
    ComponentsConfiguration.isDebugModeEnabled = true
    LithoFlipperDescriptors.add(descriptorMapping)
    client.addPlugin(InspectorFlipperPlugin(context, descriptorMapping))
    client.addPlugin(networkPlugin)
    client.addPlugin(
        SharedPreferencesFlipperPlugin(
            context,
            listOf(
                SharedPreferencesDescriptor("sample", Context.MODE_PRIVATE),
                SharedPreferencesDescriptor("other_sample", Context.MODE_PRIVATE))))
    client.addPlugin(FrescoFlipperPlugin())
    client.addPlugin(ExampleFlipperPlugin())
    client.addPlugin(CrashReporterPlugin.getInstance())
    client.addPlugin(DatabasesFlipperPlugin(context))
    client.addPlugin(NavigationFlipperPlugin.getInstance())
    val descriptorRegister = DescriptorRegister.Companion.withDefaults()
    val uidContext = UIDContext.Companion.create(context as Application)
    UIDebuggerLithoSupport.enable(uidContext)
    UIDebuggerComposeSupport.enable(uidContext)
    client.addPlugin(UIDebuggerFlipperPlugin(uidContext))
    client.start()
    val okHttpClient =
        Builder()
            .addInterceptor(interceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.MINUTES)
            .build()
    return object : InitializationResult {
      override val okHttpClient: OkHttpClient
        get() = okHttpClient
    }
  }

  interface InitializationResult {
    val okHttpClient: OkHttpClient
  }
}
