
import 'dart:async';

import 'package:flutter/services.dart';

class CS4Printer {
  static const MethodChannel _channel =
      const MethodChannel('CS4Printer');

  static Future<void> print(String documentUri, String bannerTitle) async {
    assert(documentUri != null);
    assert(bannerTitle != null);
    await _channel.invokeMethod('print', {"documentUri":documentUri, "bannerTitle":bannerTitle});
  }
}
