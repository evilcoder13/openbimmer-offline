import 'package:flutter/material.dart';
import '../models/telemetry_model.dart';
import '../services/bmw_link_service.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Live In-Car Dashboard'),
        actions: [
          StreamBuilder<String>(
            stream: BmwLinkService.instance.statusStream,
            initialData: 'DISCONNECTED',
            builder: (context, snapshot) {
              final status = snapshot.data ?? 'DISCONNECTED';
              final isConnected = status == 'CAR_READY';
              return Container(
                margin: const EdgeInsets.only(right: 16),
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: isConnected ? Colors.green.withOpacity(0.2) : Colors.red.withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.Border.all(
                    color: isConnected ? Colors.green : Colors.red,
                  ),
                ),
                child: Center(
                  child: Text(
                    status,
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: isConnected ? Colors.greenAccent : Colors.redAccent,
                    ),
                  ),
                ),
              );
            },
          ),
        ],
      ),
      body: StreamBuilder<VehicleTelemetry>(
        stream: BmwLinkService.instance.telemetryStream,
        initialData: VehicleTelemetry.empty(),
        builder: (context, snapshot) {
          final t = snapshot.data ?? VehicleTelemetry.empty();

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Vehicle Identity Card
                Card(
                  color: const Color(0xFF1E222A),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Row(
                      children: [
                        const Icon(Icons.directions_car, size: 48, color: Colors.blueAccent),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                t.model.isNotEmpty ? t.model : 'BMW Connected Vehicle',
                                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                t.vin.isNotEmpty ? 'VIN: ${t.vin}' : 'Waiting for Bluetooth connection...',
                                style: TextStyle(color: Colors.grey.shade400, fontSize: 13),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Speed & Odometer Card
                Row(
                  children: [
                    Expanded(
                      child: _buildMetricCard(
                        title: 'SPEED',
                        value: '${t.speedKmh.toInt()}',
                        unit: 'km/h',
                        icon: Icons.speed,
                        color: Colors.cyanAccent,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildMetricCard(
                        title: 'ODOMETER',
                        value: '${t.odometerKm}',
                        unit: 'km',
                        icon: Icons.av_timer,
                        color: Colors.amberAccent,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),

                // Fuel & Battery Level
                Row(
                  children: [
                    Expanded(
                      child: _buildMetricCard(
                        title: 'FUEL TANK',
                        value: '${t.fuelPercent}%',
                        unit: '(${t.fuelLiters.toStringAsFixed(1)} L)',
                        icon: Icons.local_gas_station,
                        color: Colors.orangeAccent,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildMetricCard(
                        title: 'EV BATTERY',
                        value: '${t.batteryPercent}%',
                        unit: 'State of Charge',
                        icon: Icons.battery_charging_full,
                        color: Colors.lightGreenAccent,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // Status Indicators
                Card(
                  color: const Color(0xFF1E222A),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'VEHICLE CAN-BUS SENSORS',
                          style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 1.2, color: Colors.grey),
                        ),
                        const Divider(height: 24),
                        _buildStatusRow('Parking Brake', t.parkingBrake ? 'ENGAGED' : 'RELEASED', t.parkingBrake ? Colors.redAccent : Colors.greenAccent),
                        const SizedBox(height: 12),
                        _buildStatusRow('Vehicle Antenna GPS', t.latitude != 0.0 ? '${t.latitude.toStringAsFixed(4)}, ${t.longitude.toStringAsFixed(4)}' : 'Standby', Colors.white70),
                        const SizedBox(height: 12),
                        _buildStatusRow('Cloud Connectivity', 'ZERO CLOUD (100% Offline)', Colors.cyanAccent),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildMetricCard({
    required String title,
    required String value,
    required String unit,
    required IconData icon,
    required Color color,
  }) {
    return Card(
      color: const Color(0xFF1E222A),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 20, color: color),
                const SizedBox(width: 8),
                Text(title, style: TextStyle(color: Colors.grey.shade400, fontSize: 12, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 12),
            Text(value, style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: color)),
            Text(unit, style: TextStyle(color: Colors.grey.shade500, fontSize: 12)),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusRow(String label, String value, Color color) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontSize: 14, color: Colors.white70)),
        Text(value, style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: color)),
      ],
    );
  }
}
