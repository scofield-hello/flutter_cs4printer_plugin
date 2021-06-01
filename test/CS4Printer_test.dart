import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:CS4Printer/CS4Printer.dart';

void main() {
  const MethodChannel channel = MethodChannel('CS4Printer');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await CS4Printer.platformVersion, '42');
  });
}
