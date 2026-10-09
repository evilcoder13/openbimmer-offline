import 'package:flutter/material.dart';
import '../services/bmw_link_service.dart';

class DevicesScreen extends StatefulWidget {
  const DevicesScreen({super.key});

  @override
  State<DevicesScreen> createState() => _DevicesScreenState();
}

class _DevicesScreenState extends State<DevicesScreen> {
  List<Map<String, String>> _devices = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadDevices();
  }

  Future<void> _loadDevices() async {
    setState(() => _isLoading = true);
    final list = await BmwLinkService.instance.getPairedBmwDevices();
    setState(() {
      _devices = list;
      _isLoading = false;
    });
  }

  Future<void> _connect(String address) async {
    final messenger = ScaffoldMessenger.of(context);
    messenger.showSnackBar(
      SnackBar(content: Text('Initiating Bluetooth SPP handshake to $address...')),
    );
    await BmwLinkService.instance.connectToDevice(address);
  }

  Future<void> _disconnect() async {
    await BmwLinkService.instance.disconnect();
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Disconnected from vehicle.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Paired BMW Vehicles'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadDevices,
            tooltip: 'Refresh paired devices',
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _devices.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.bluetooth_searching, size: 64, color: Colors.grey),
                      const SizedBox(height: 16),
                      const Text(
                        'No paired BMW devices found',
                        style: TextStyle(fontSize: 16, color: Colors.grey),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Pair your smartphone with BMW iDrive in Android Bluetooth Settings first.',
                        style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(16.0),
                  itemCount: _devices.length,
                  itemBuilder: (context, index) {
                    final d = _devices[index];
                    final name = d['name'] ?? 'Unknown';
                    final address = d['address'] ?? '';
                    final isBmw = name.toLowerCase().contains('bmw') || name.toLowerCase().contains('mini');

                    return Card(
                      color: const Color(0xFF1E222A),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      margin: const EdgeInsets.only(bottom: 12),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: isBmw ? Colors.blueAccent.withOpacity(0.2) : Colors.grey.withOpacity(0.2),
                          child: Icon(
                            isBmw ? Icons.directions_car : Icons.bluetooth,
                            color: isBmw ? Colors.blueAccent : Colors.grey,
                          ),
                        ),
                        title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
                        subtitle: Text(address, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.green,
                                foregroundColor: Colors.white,
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              ),
                              onPressed: () => _connect(address),
                              child: const Text('Connect'),
                            ),
                            const SizedBox(width: 8),
                            IconButton(
                              icon: const Icon(Icons.stop_circle_outlined, color: Colors.redAccent),
                              tooltip: 'Disconnect',
                              onPressed: _disconnect,
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
