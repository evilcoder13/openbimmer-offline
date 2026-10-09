# OpenBimmer Offline 🚗💨

**OpenBimmer Offline** là giải pháp mã nguồn mở kết nối điện thoại Android trực tiếp với hệ thống giải trí BMW / MINI iDrive (NBT / NBT Evo / EntryNav / CIC) mà **hoàn toàn KHÔNG cần kết nối Internet** hay phụ thuộc vào cloud backend BMW (`b2vapi.bmwgroup.com`).

---

## ✨ Tính năng nổi bật (Key Features)

1. **100% Offline BCL Protocol Engine**:
   - Tự động phát hiện và kết nối Bluetooth SPP (Serial Port Profile - UUID: `00001101-0000-1000-8000-00805F9B34FB`) với xe BMW đã ghép đôi.
   - Triển khai trực tiếp BMW BCL Layer (Framing, Handshake Syn/Ack, Reset, Dispatch) trên thiết bị.
   - Độc lập hoàn toàn, không gửi bất kỳ thông tin nào ra ngoài Internet, không thu thập dữ liệu cá nhân.

2. **In-Car Vehicle Telemetry Dashboard**:
   - Đẩy ứng dụng Telemetry trực tiếp lên màn hình iDrive của xe thông qua BMW App Framework.
   - Hiển thị trực quan: Tốc độ tức thời (Speed km/h), Vòng tua máy (RPM), Mức nhiên liệu (Fuel Tank %), Quãng đường (Mileage km), Nhiệt độ dầu/nước làm mát máy (Coolant/Oil Temp).

3. **Android App Mirror / Linked App Frame**:
   - Cho phép chọn bất kỳ ứng dụng Android nào trên điện thoại (Google Maps, Spotify, Zing MP3, YouTube Music, ...) để liên kết và render frame thông tin / trạng thái lên màn hình iDrive.

4. **Hybrid Navigation & Destination Dispatch**:
   - Tra cứu và gửi toạ độ GPS (Latitude, Longitude, Destination Name) thẳng vào hệ thống bản đồ gốc của xe BMW iDrive.
   - **Offline POI Search**: Tích hợp sẵn cơ sở dữ liệu các địa điểm trọng điểm (Sân bay, Trung tâm thành phố, Showroom BMW, Địa danh nổi tiếng) với tính năng **Auto-complete / Gợi ý tự động**.
   - **Online Fallback**: Tự động tra cứu mở rộng qua Android Geocoder / Nominatim khi điện thoại có kết nối 4G/Wifi.

---

## 🛠️ Cấu trúc dự án (Architecture)

```
flutter_idrive_offline/
├── .gitignore
├── README.md
└── android/
    ├── app/
    │   ├── src/
    │   │   └── main/
    │   │       ├── AndroidManifest.xml
    │   │       ├── assets/
    │   │       │   └── offline_places.json       # Cơ sở dữ liệu POI offline
    │   │       ├── kotlin/com/openbimmer/idrive/
    │   │       │   ├── MainActivity.kt           # Giao diện chính, POI Autocomplete & Telemetry handler
    │   │       │   ├── bcl/                      # Bộ giải mã BMW BCL Protocol (Syn/Ack/Data frames)
    │   │       │   └── service/FixedBtService.kt # Dịch vụ Bluetooth SPP background
    │   │       └── res/layout/activity_main.xml  # Giao diện Android Native UI
    │   └── build.gradle
    ├── build.gradle
    └── settings.gradle
```

---

## 🚀 Hướng dẫn cài đặt & Build (Getting Started)

### Yêu cầu môi trường:
- Android SDK (API 34, Build Tools 34.0.0+)
- JDK 17 hoặc JDK 21+
- Thiết bị Android 8.0 (API 26) trở lên hỗ trợ Bluetooth.

### Lệnh biên dịch APK:
```bash
cd android
./gradlew assembleDebug
```
File APK sẽ được tạo tại: `android/app/build/outputs/apk/debug/app-debug.apk`.

### Cài đặt lên điện thoại:
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📖 Hướng dẫn sử dụng trên xe

1. Bật Bluetooth trên điện thoại và ghép đôi (Pair) với xe BMW/MINI qua iDrive Bluetooth menu.
2. Mở ứng dụng **OpenBimmer Offline** trên điện thoại.
3. Bấm **"Auto-Connect to Paired BMW"**:
   - Ứng dụng sẽ tìm xe BMW đã kết nối và thiết lập kênh BCL RFCOMM.
4. Sử dụng các tính năng:
   - **Vehicle Telemetry**: Xem các thông số cảm biến xe trên màn hình iDrive.
   - **Link Android App to iDrive**: Chọn ứng dụng nhạc/bản đồ cần đồng bộ.
   - **Offline Navigation**: Gõ tên điểm đến (ví dụ: *Nội Bài, Tân Sơn Nhất, Hồ Gươm...*), ứng dụng sẽ tự động gợi ý toạ độ và bấm **"Send to BMW iDrive Nav"** để xe bắt đầu dẫn đường.

---

## ⚖️ Giấy phép (License)
Dự án được phân phối dưới giấy phép [MIT License](LICENSE).
Mọi quyền sở hữu thương hiệu BMW, iDrive thuộc về Bayerische Motoren Werke AG. Dự án này được nghiên cứu và phát triển độc lập cho mục đích học tập và tương thích thiết bị.
