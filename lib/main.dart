import 'package:flutter/material.dart';
import 'screens/dashboard_screen.dart';
import 'screens/offline_navigation_screen.dart';
import 'screens/devices_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const OpenBimmerApp());
}

class OpenBimmerApp extends StatelessWidget {
  const OpenBimmerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OpenBimmer Offline',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: const Color(0xFF12151B),
        primaryColor: Colors.blueAccent,
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF181C24),
          elevation: 0,
          titleTextStyle: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
        ),
        colorScheme: const ColorScheme.dark(
          primary: Colors.blueAccent,
          secondary: Colors.cyanAccent,
          surface: Color(0xFF1E222A),
        ),
      ),
      home: const MainNavigationShell(),
    );
  }
}

class MainNavigationShell extends StatefulWidget {
  const MainNavigationShell({super.key});

  @override
  State<MainNavigationShell> createState() => _MainNavigationShellState();
}

class _MainNavigationShellState extends State<MainNavigationShell> {
  int _currentIndex = 0;

  final List<Widget> _screens = const [
    DashboardScreen(),
    OfflineNavigationScreen(),
    DevicesScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_currentIndex],
      bottomNavigationBar: NavigationBar(
        backgroundColor: const Color(0xFF181C24),
        indicatorColor: Colors.blueAccent.withOpacity(0.3),
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() => _currentIndex = index);
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            selectedIcon: Icon(Icons.dashboard, color: Colors.blueAccent),
            label: 'Dashboard',
          ),
          NavigationDestination(
            icon: Icon(Icons.navigation_outlined),
            selectedIcon: Icon(Icons.navigation, color: Colors.blueAccent),
            label: 'Navigation',
          ),
          NavigationDestination(
            icon: Icon(Icons.bluetooth_outlined),
            selectedIcon: Icon(Icons.bluetooth, color: Colors.blueAccent),
            label: 'Vehicles',
          ),
        ],
      ),
    );
  }
}
