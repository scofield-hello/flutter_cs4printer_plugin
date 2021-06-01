import 'package:CS4Printer/CS4Printer.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: FlatButton.icon(
              onPressed: () {
                CS4Printer.print("https://baidu.com", "廊坊市司法局自助终端");
              },
              icon: Icon(Icons.print),
              label: Text("打印")),
        ),
      ),
    );
  }
}
