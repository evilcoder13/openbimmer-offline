class OfflineDestination {
  final String id;
  final String title;
  final String address;
  final double latitude;
  final double longitude;
  final bool isFavorite;

  const OfflineDestination({
    required this.id,
    required this.title,
    required this.address,
    required this.latitude,
    required this.longitude,
    this.isFavorite = false,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'title': title,
      'address': address,
      'latitude': latitude,
      'longitude': longitude,
      'isFavorite': isFavorite,
    };
  }

  factory OfflineDestination.fromMap(Map<String, dynamic> map) {
    return OfflineDestination(
      id: map['id'] as String,
      title: map['title'] as String,
      address: map['address'] as String,
      latitude: (map['latitude'] as num).toDouble(),
      longitude: (map['longitude'] as num).toDouble(),
      isFavorite: map['isFavorite'] as bool? ?? false,
    );
  }
}
