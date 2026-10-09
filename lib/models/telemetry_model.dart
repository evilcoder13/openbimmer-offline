class VehicleTelemetry {
  final String vin;
  final String model;
  final double speedKmh;
  final double fuelLiters;
  final int fuelPercent;
  final int batteryPercent;
  final int odometerKm;
  final double latitude;
  final double longitude;
  final bool parkingBrake;
  final DateTime timestamp;

  const VehicleTelemetry({
    this.vin = '',
    this.model = '',
    this.speedKmh = 0.0,
    this.fuelLiters = 0.0,
    this.fuelPercent = 0,
    this.batteryPercent = 0,
    this.odometerKm = 0,
    this.latitude = 0.0,
    this.longitude = 0.0,
    this.parkingBrake = false,
    required this.timestamp,
  });

  factory VehicleTelemetry.fromMap(Map<dynamic, dynamic> map) {
    return VehicleTelemetry(
      vin: map['vin'] as String? ?? '',
      model: map['model'] as String? ?? '',
      speedKmh: (map['speedKmh'] as num?)?.toDouble() ?? 0.0,
      fuelLiters: (map['fuelLiters'] as num?)?.toDouble() ?? 0.0,
      fuelPercent: (map['fuelPercent'] as num?)?.toInt() ?? 0,
      batteryPercent: (map['batteryPercent'] as num?)?.toInt() ?? 0,
      odometerKm: (map['odometerKm'] as num?)?.toInt() ?? 0,
      latitude: (map['latitude'] as num?)?.toDouble() ?? 0.0,
      longitude: (map['longitude'] as num?)?.toDouble() ?? 0.0,
      parkingBrake: map['parkingBrake'] as bool? ?? false,
      timestamp: DateTime.fromMillisecondsSinceEpoch(
        (map['timestamp'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      ),
    );
  }

  factory VehicleTelemetry.empty() {
    return VehicleTelemetry(timestamp: DateTime.now());
  }
}
