import 'dart:async';
import 'package:flutter/services.dart';
import '../models/telemetry_model.dart';

class BmwLinkService {
  static const MethodChannel _methodChannel = MethodChannel('com.openbimmer.idrive/methods');
  static const EventChannel _telemetryChannel = EventChannel('com.openbimmer.idrive/telemetry');
  static const EventChannel _statusChannel = EventChannel('com.openbimmer.idrive/status');

  static final BmwLinkService instance = BmwLinkService._internal();
  BmwLinkService._internal();

  Stream<VehicleTelemetry>? _telemetryStream;
  Stream<String>? _statusStream;

  Stream<VehicleTelemetry> get telemetryStream {
    _telemetryStream ??= _telemetryChannel
        .receiveBroadcastStream()
        .map((event) => VehicleTelemetry.fromMap(event as Map<dynamic, dynamic>));
    return _telemetryStream!;
  }

  Stream<String> get statusStream {
    _statusStream ??= _statusChannel
        .receiveBroadcastStream()
        .map((event) => event as String);
    return _statusStream!;
  }

  Future<List<Map<String, String>>> getPairedBmwDevices() async {
    try {
      final List<dynamic>? devices = await _methodChannel.invokeMethod('getPairedBmwDevices');
      if (devices == null) return [];
      return devices.map((d) {
        final m = d as Map<dynamic, dynamic>;
        return {
          'name': m['name'] as String? ?? 'Unknown',
          'address': m['address'] as String? ?? '',
        };
      }).toList();
    } catch (e) {
      return [];
    }
  }

  Future<bool> connectToDevice(String address) async {
    try {
      final bool? result = await _methodChannel.invokeMethod('connectToDevice', {'address': address});
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  Future<bool> disconnect() async {
    try {
      final bool? result = await _methodChannel.invokeMethod('disconnect');
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  Future<bool> sendRouteToCar(String name, double latitude, double longitude) async {
    try {
      final bool? result = await _methodChannel.invokeMethod('sendRouteToCar', {
        'name': name,
        'lat': latitude,
        'lng': longitude,
      });
      return result ?? false;
    } catch (e) {
      return false;
    }
  }

  Future<bool> pushNavigationCardToCar(String instruction, int distanceMeters) async {
    try {
      final bool? result = await _methodChannel.invokeMethod('pushNavigationCardToCar', {
        'instruction': instruction,
        'distance': distanceMeters,
      });
      return result ?? false;
    } catch (e) {
      return false;
    }
  }
}
