import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'config/app_config.dart';
import 'screens/home_screen.dart';
import 'state/app_controller.dart';
import 'state/call_controller.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppConfig.load();

  runApp(const MyApp());
}

/// Root widget configuring dark Material 3 theming and application state providers.
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<CallController>(
          create: (_) => CallController(),
        ),
        ChangeNotifierProvider<AppController>(
          create: (_) => AppController()..init(),
        ),
      ],
      child: MaterialApp(
        title: 'Ayer',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          brightness: Brightness.dark,
        ),
        home: const HomeScreen(),
      ),
    );
  }
}
