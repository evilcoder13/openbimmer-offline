import 'package:flutter/material.dart';
import '../models/destination_model.dart';
import '../services/bmw_link_service.dart';

class OfflineNavigationScreen extends StatefulWidget {
  const OfflineNavigationScreen({super.key});

  @override
  State<OfflineNavigationScreen> createState() => _OfflineNavigationScreenState();
}

class _OfflineNavigationScreenState extends State<OfflineNavigationScreen> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _latController = TextEditingController();
  final TextEditingController _lngController = TextEditingController();

  final List<OfflineDestination> _savedDestinations = [
    const OfflineDestination(
      id: '1',
      title: 'Home',
      address: 'Private Residence',
      latitude: 21.0285,
      longitude: 105.8542,
      isFavorite: true,
    ),
    const OfflineDestination(
      id: '2',
      title: 'Office / Workplace',
      address: 'Business Center',
      latitude: 21.0333,
      longitude: 105.8167,
      isFavorite: true,
    ),
    const OfflineDestination(
      id: '3',
      title: 'BMW Service Center',
      address: 'Authorized Service Workshop',
      latitude: 21.0068,
      longitude: 105.8431,
      isFavorite: false,
    ),
  ];

  Future<void> _sendToVehicle(OfflineDestination dest) async {
    final messenger = ScaffoldMessenger.of(context);
    final success = await BmwLinkService.instance.sendRouteToCar(
      dest.title,
      dest.latitude,
      dest.longitude,
    );

    messenger.showSnackBar(
      SnackBar(
        content: Text(
          success
              ? 'Destination "${dest.title}" sent directly to iDrive navigation!'
              : 'Failed to send: Ensure vehicle Bluetooth is connected.',
        ),
        backgroundColor: success ? Colors.green : Colors.red,
      ),
    );
  }

  void _showAddDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1E222A),
        title: const Text('Add Offline Destination'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: _nameController,
              decoration: const InputDecoration(labelText: 'Destination Name'),
            ),
            TextField(
              controller: _latController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Latitude (e.g. 21.0285)'),
            ),
            TextField(
              controller: _lngController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Longitude (e.g. 105.8542)'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              final name = _nameController.text.trim();
              final lat = double.tryParse(_latController.text.trim()) ?? 0.0;
              final lng = double.tryParse(_lngController.text.trim()) ?? 0.0;
              if (name.isNotEmpty && lat != 0.0 && lng != 0.0) {
                setState(() {
                  _savedDestinations.add(
                    OfflineDestination(
                      id: DateTime.now().millisecondsSinceEpoch.toString(),
                      title: name,
                      address: '$lat, $lng',
                      latitude: lat,
                      longitude: lng,
                    ),
                  );
                });
                _nameController.clear();
                _latController.clear();
                _lngController.clear();
                Navigator.pop(ctx);
              }
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Offline Navigation Dispatch'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_location_alt),
            onPressed: _showAddDialog,
            tooltip: 'Add Custom Coordinate',
          ),
        ],
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16.0),
        itemCount: _savedDestinations.length,
        itemBuilder: (context, index) {
          final dest = _savedDestinations[index];
          return Card(
            color: const Color(0xFF1E222A),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            margin: const EdgeInsets.only(bottom: 12),
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              leading: CircleAvatar(
                backgroundColor: Colors.blueAccent.withOpacity(0.2),
                child: Icon(
                  dest.isFavorite ? Icons.star : Icons.place,
                  color: dest.isFavorite ? Colors.amberAccent : Colors.blueAccent,
                ),
              ),
              title: Text(
                dest.title,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              subtitle: Text(
                '${dest.address}\n(${dest.latitude.toStringAsFixed(4)}, ${dest.longitude.toStringAsFixed(4)})',
                style: TextStyle(color: Colors.grey.shade400, fontSize: 12),
              ),
              isThreeLine: true,
              trailing: ElevatedButton.icon(
                icon: const Icon(Icons.send_to_mobile, size: 16),
                label: const Text('Send to Car'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blueAccent,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                onPressed: () => _sendToVehicle(dest),
              ),
            ),
          );
        },
      ),
    );
  }
}
