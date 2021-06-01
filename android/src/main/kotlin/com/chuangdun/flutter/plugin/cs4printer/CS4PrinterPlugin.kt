package com.chuangdun.flutter.plugin.cs4printer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

private const val TAG = "CS4PrinterPlugin"

/** CS4PrinterPlugin */
class CS4PrinterPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel : MethodChannel
  private lateinit var context:Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext;
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "CS4Printer")
    channel.setMethodCallHandler(this)

  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    Log.i(TAG, "打印机调用:${call.method}, 参数:${call.arguments}")
    if (call.method == "print") {
      val arguments = call.arguments as Map<*, *>
      val documentUri = arguments["documentUri"] as String
      val bannerTitle = arguments["bannerTitle"] as String
      val intent = Intent(context, PrintActivity::class.java)
      intent.putExtra("documentUri", documentUri)
      intent.putExtra("bannerTitle", bannerTitle)
      context.startActivity(intent)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
