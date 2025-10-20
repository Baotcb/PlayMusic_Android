# Music Playback Foreground Service - Exercise 1

## Mô tả
Ứng dụng Android này minh họa cách sử dụng **Foreground Service** để download một file ảnh từ mạng và hiển thị lên màn hình. Service chạy ở foreground với notification để người dùng biết service đang hoạt động.

## Tính năng

### 1. Foreground Service
- Service chạy ở foreground với notification
- Notification hiển thị trạng thái của service (downloading, downloaded, etc.)
- Service type: `mediaPlayback` (phù hợp cho ứng dụng phát nhạc)

### 2. Download Image từ Internet
- Download ảnh từ URL: `https://picsum.photos/800/600`
- Sử dụng `ExecutorService` để download ảnh trong background thread
- Không block UI thread

### 3. Hiển thị Image
- Hiển thị ảnh đã download lên `ImageView`
- Sử dụng `LocalBroadcastManager` để gửi thông báo khi download xong
- Bind service để truy xuất bitmap đã download

### 4. Runtime Permissions
- Request permission `POST_NOTIFICATIONS` cho Android 13+ (API 33+)
- Request permission tự động khi user nhấn nút Start Service

## Các thành phần chính

### MusicPlaybackService.java
- Service chạy ở foreground
- Download ảnh từ internet
- Tạo và quản lý notification
- Gửi broadcast khi download hoàn tất

### MusicBinder.java
- Binder class để bind service với Activity
- Cho phép Activity truy xuất service instance

### MainActivity.java
- UI chính của app
- Start/Stop service
- Nhận broadcast từ service
- Hiển thị ảnh đã download

### Layout & Resources
- `activity_main.xml`: Layout với ImageView, TextView, và 2 Buttons
- `strings.xml`: Các string resources
- `AndroidManifest.xml`: Khai báo permissions và service

## Permissions được sử dụng

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Cách sử dụng

1. **Build và Run** ứng dụng trên thiết bị hoặc emulator
2. Nhấn nút **"Start Music Service"**
   - Với Android 13+: App sẽ request notification permission
   - Service sẽ start và notification xuất hiện
   - Ảnh sẽ được download tự động
3. Xem ảnh hiển thị trên màn hình sau khi download xong
4. Nhấn nút **"Stop Music Service"** để dừng service

## Kiến trúc kỹ thuật

### Service Lifecycle
1. `onCreate()`: Khởi tạo ExecutorService, Handler, tạo notification channel
2. `onStartCommand()`: Start foreground, bắt đầu download ảnh
3. `onBind()`: Trả về Binder để Activity có thể truy xuất service
4. `onDestroy()`: Cleanup resources, shutdown ExecutorService

### Communication Flow
```
MainActivity → Start Service → MusicPlaybackService
                                      ↓
                                Download Image
                                      ↓
                                LocalBroadcast ← MainActivity
                                      ↓
                                Display Image
```

### Threading Model
- **Main Thread**: UI operations, notification updates
- **Background Thread**: Network operations (download image)
- **Handler**: Post results từ background thread về main thread

## Lưu ý quan trọng

1. **Internet Permission**: Cần có INTERNET permission trong manifest
2. **Foreground Service**: Phải start foreground trong 5 giây sau `onStartCommand()`
3. **Notification Channel**: Bắt buộc từ Android 8.0 (API 26+)
4. **Service Type**: Phải khai báo `foregroundServiceType` trong manifest
5. **LocalBroadcastManager**: Deprecated nhưng vẫn hoạt động tốt cho internal broadcasts

## Cải tiến có thể thực hiện

- [ ] Sử dụng WorkManager thay vì Service cho background tasks
- [ ] Thêm progress bar hiển thị tiến trình download
- [ ] Cache ảnh đã download vào storage
- [ ] Thêm media player thực sự để phát nhạc
- [ ] Sử dụng modern alternatives như Glide hoặc Coil cho image loading
- [ ] Replace LocalBroadcastManager bằng LiveData hoặc Flow

## Yêu cầu hệ thống

- **minSdk**: 27 (Android 8.1)
- **targetSdk**: 36
- **Internet connection**: Cần kết nối internet để download ảnh

## Dependencies

```kotlin
implementation("androidx.appcompat:appcompat")
implementation("com.google.android.material:material")
implementation("androidx.activity:activity")
implementation("androidx.constraintlayout:constraintlayout")
implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
```
