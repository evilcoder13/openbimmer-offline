# OpenBimmer Offline 🚗💨

[**English**](#english-version) • [**Tiếng Việt**](#phiên-bản-tiếng-việt)

---

<a name="english-version"></a>
# English Version

**OpenBimmer Offline** is an open-source, privacy-focused solution that connects Android smartphones directly to BMW / MINI iDrive infotainment systems (NBT / NBT Evo / EntryNav / CIC) with **100% OFFLINE operation and ZERO CLOUD dependency** (completely eliminating `b2vapi.bmwgroup.com`).

---

## ✨ Key Features

1. **100% Offline BCL Protocol Engine**:
   - Automatic discovery and connection via Bluetooth SPP (Serial Port Profile - UUID: `00001101-0000-1000-8000-00805F9B34FB`) with previously paired BMW vehicles.
   - Native on-device implementation of BMW BCL Layer (Framing, Handshake Syn/Ack, Reset, Dispatch).
   - Zero telemetry sent outside, no BMW ConnectedDrive account required, complete privacy.

2. **In-Car Vehicle Telemetry Dashboard**:
   - Pushes an M-style digital gauge and telemetry dashboard directly onto the BMW iDrive central screen.
   - Real-time data parsing: Speed (km/h), RPM, Fuel tank level (%), Odometer (km), and Engine coolant/oil temperatures.

3. **Android App Mirror / Linked App Frame**:
   - Supports selecting any installed Android app (Spotify, Google Maps, Waze, Zing MP3, YouTube Music, etc.) to link and stream custom UI cards directly to the BMW widescreen.

4. **Hybrid Navigation & Destination Dispatch**:
   - Resolves and dispatches GPS coordinates directly into BMW native HDD Navigation (Channel 3).
   - **Offline POI Search**: Preloaded offline POI seed database with instant **Auto-complete suggestions**.
   - **Online Fallback**: Automatic background lookup via Geocoder / OpenStreetMap Nominatim when cellular data is available.

5. **Multi-Language Support (EN / VN)**:
   - Built-in instant language switcher on the mobile app interface between English and Vietnamese.

---

## 🛠️ Architecture

```
flutter_idrive_offline/
├── .github/workflows/        # CI/CD automated build pipelines
├── docs/images/momo-qr.png   # Sponsorship & Donation QR
├── README.md                 # Bilingual documentation (EN / VN)
└── android/
    ├── app/
    │   ├── src/
    │   │   └── main/
    │   │       ├── AndroidManifest.xml       # Full package visibility & Bluetooth configs
    │   │       ├── assets/
    │   │       │   └── offline_places.json   # Offline POI database
    │   │       ├── kotlin/com/openbimmer/idrive/
    │   │       │   ├── MainActivity.kt       # Multi-language UI, POI Search & Link controller
    │   │       │   ├── bcl/                  # BMW BCL framing & packet codec
    │   │       │   └── service/FixedBtService.kt # Foreground Bluetooth SPP service
    │   │       └── res/layout/activity_main.xml
    │   └── build.gradle
    ├── build.gradle
    └── settings.gradle
```

---

## 🚀 Getting Started & Build

### Prerequisites:
- Android SDK (API 34, Build Tools 34.0.0+)
- JDK 17 or JDK 21+
- Android Device (Android 8.0 / API 26+) with Bluetooth enabled.

### Build APK locally:
```bash
cd android
./gradlew assembleDebug
```
The compiled APK will be located at: `android/app/build/outputs/apk/debug/app-debug.apk`.

### Install to device via ADB:
```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📖 How to Use in Car

1. Pair your Android phone with your BMW via the car's native iDrive Bluetooth menu.
2. Launch **OpenBimmer Offline** on your phone.
3. Tap **"Auto-Connect Paired BMW"**: The app connects to the car's RFCOMM port and initializes BCL.
4. Enjoy offline features:
   - View live sensors on iDrive screen with **"Launch Telemetry App"**.
   - Select an app (Spotify, Maps, etc.) and stream UI cards.
   - Type destination name (auto-complete supported) and tap **"Push Coordinates to iDrive Navigation"**.

---

## ☕ Support & Donations (Buy Me a Coffee)

If **OpenBimmer Offline** saves you time, keeps your BMW connected without expensive cloud subscriptions, or adds cool tech to your dashboard:

You can support continued development and bug fixes here:

* 🌍 **PayPal**: [paypal.me/evilcoder13](https://paypal.me/evilcoder13) (`evilcoder13`)
* 🇻🇳 **MoMo / Fast Bank Transfer**: `0989993597`

<p align="center">
  <img src="docs/images/momo-qr.png" alt="MoMo QR Code" width="220" />
  <br>
  <em>Thank you for your generous support! Wishing you safe and happy drives! 🎉</em>
</p>

---

## ⚖️ License
Distributed under the [MIT License](LICENSE).
BMW and iDrive are registered trademarks of Bayerische Motoren Werke AG. This project is independently developed for educational and interoperability purposes.

---
---

<a name="phiên-bản-tiếng-việt"></a>
# Phiên Bản Tiếng Việt

**OpenBimmer Offline** là giải pháp mã nguồn mở kết nối điện thoại Android trực tiếp với hệ thống giải trí BMW / MINI iDrive (NBT / NBT Evo / EntryNav / CIC) mà **hoàn toàn KHÔNG cần kết nối Internet** hay phụ thuộc vào cloud backend BMW (`b2vapi.bmwgroup.com`).

---

## ✨ Tính năng nổi bật

1. **100% Offline BCL Protocol Engine**:
   - Tự động phát hiện và kết nối Bluetooth SPP (Serial Port Profile - UUID: `00001101-0000-1000-8000-00805F9B34FB`) với xe BMW đã ghép đôi.
   - Triển khai trực tiếp BMW BCL Layer (Framing, Handshake Syn/Ack, Reset, Dispatch) ngay trên thiết bị.
   - Hoàn toàn bảo mật, không gửi dữ liệu ra bên ngoài, không cần tài khoản BMW ConnectedDrive.

2. **In-Car Vehicle Telemetry Dashboard**:
   - Đẩy ứng dụng đồng hồ kỹ thuật số phong cách M-Power trực tiếp lên màn hình trung tâm iDrive.
   - Đọc dữ liệu xe theo thời gian thực: Tốc độ (km/h), Vòng tua máy (RPM), Mức nhiên liệu (%), ODO (km), Nhiệt độ dầu/nước làm mát.

3. **Android App Mirror / Linked App Frame**:
   - Cho phép chọn bất kỳ ứng dụng nào trên điện thoại (Spotify, Google Maps, Waze, Zing MP3, YouTube Music...) để liên kết và render khung thông tin lên màn hình xe.

4. **Hybrid Navigation & Destination Dispatch**:
   - Bắn toạ độ GPS thẳng vào hệ thống bản đồ dẫn đường gốc của xe iDrive qua Channel 3.
   - **Offline POI Search**: Tích hợp cơ sở dữ liệu địa điểm ngoại tuyến với tính năng **Gợi ý tự động (Auto-complete)**.
   - **Online Fallback**: Tự động tra cứu mở rộng qua Geocoder / OSM Nominatim khi điện thoại có mạng 4G/Wifi.

5. **Hỗ trợ Đa Ngôn Ngữ (EN / VN)**:
   - Tích hợp nút chuyển đổi trực tiếp giữa tiếng Anh (EN) và tiếng Việt (VN) ngay trên giao diện ứng dụng.

---

## 🚀 Cài đặt & Biên dịch

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

## ☕ Mời Mình Ly Cà Phê Nhé! (Buy Me a Coffee)

Nếu dự án **OpenBimmer Offline** giúp chiếc xe BMW của bạn thông minh hơn mà không tốn phí dịch vụ cloud hàng năm:

Bạn có thể tiếp thêm chút năng lượng cho tác giả duy trì và phát triển thêm tính năng tại:

* 🌍 **PayPal**: [paypal.me/evilcoder13](https://paypal.me/evilcoder13) (`evilcoder13`)
* 🇻🇳 **MoMo / Chuyển khoản nhanh**: `0989993597`

<p align="center">
  <img src="docs/images/momo-qr.png" alt="MoMo QR Code" width="220" />
  <br>
  <em>Cảm ơn sự đồng hành và ủng hộ của bạn! Chúc bạn vạn dặm bình an! 🎉</em>
</p>
