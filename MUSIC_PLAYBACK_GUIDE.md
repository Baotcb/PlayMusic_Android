# 🎵 Music Playback Service - Hướng dẫn sử dụng

## Tính năng đã thêm

### ✅ Phát nhạc thực sự
- Service bây giờ **phát nhạc thực sự** từ file `cung_anh.mp3`
- Sử dụng **MediaPlayer** để phát nhạc
- Nhạc sẽ **tự động lặp lại** (loop) khi hết bài

## Cách hoạt động

### 1️⃣ Khi bấm nút "Start Music Service"
- ✅ Service khởi động ở foreground
- ✅ **Nhạc bắt đầu phát** 🎵
- ✅ Notification hiển thị "Playing music..."
- ✅ Download ảnh từ internet
- ✅ Hiển thị ảnh lên màn hình

### 2️⃣ Khi bấm nút "Stop Music Service"
- ✅ **Nhạc dừng lại** 🛑
- ✅ Service tắt
- ✅ Notification biến mất
- ✅ Ảnh bị xóa khỏi màn hình

## File nhạc

### Vị trí file
```
app/src/main/res/raw/cung_anh.mp3
```

### Đổi bài hát khác
Nếu muốn đổi bài hát khác:

1. **Copy file .mp3** vào thư mục `app/src/main/res/raw/`
2. **Đặt tên file** theo quy tắc:
   - Chỉ dùng chữ thường: `a-z`
   - Chỉ dùng số: `0-9`
   - Chỉ dùng gạch dưới: `_`
   - VÍ DỤ: `bai_hat_moi.mp3` ✅
   - SAI: `Bài Hát Mới.mp3` ❌

3. **Cập nhật code** trong `MusicPlaybackService.java`:
```java
mediaPlayer = MediaPlayer.create(this, R.raw.bai_hat_moi);
```

## Luồng hoạt động

```
User bấm Start
      ↓
[Kiểm tra permission]
      ↓
[Service start]
      ↓
[🎵 BẮT ĐẦU PHÁT NHẠC]
      ↓
[Download ảnh]
      ↓
[Hiển thị ảnh]
      ↓
[Nhạc tiếp tục phát (loop)]
      ↓
User bấm Stop
      ↓
[🛑 DỪNG NHẠC]
      ↓
[Service stop]
```

## Chạy ứng dụng

### Build APK
```powershell
.\gradlew assembleDebug
```

### Install lên thiết bị
```powershell
.\gradlew installDebug
```

Hoặc chạy từ Android Studio bằng nút **Run** (▶️)

## Test

### Test Case 1: Phát nhạc
1. Mở app
2. Bấm "Start Music Service"
3. ✅ Nghe thấy nhạc phát
4. ✅ Thấy notification
5. ✅ Thấy ảnh hiển thị (sau vài giây)

### Test Case 2: Dừng nhạc
1. Sau khi nhạc đang phát
2. Bấm "Stop Music Service"
3. ✅ Nhạc dừng ngay lập tức
4. ✅ Notification biến mất
5. ✅ Ảnh bị xóa

### Test Case 3: Background
1. Bấm Start Service (nhạc phát)
2. Bấm Home (app vào background)
3. ✅ Nhạc vẫn tiếp tục phát
4. ✅ Notification vẫn hiển thị
5. Tap notification → Quay lại app
6. ✅ Nhạc vẫn đang phát

### Test Case 4: Loop
1. Bấm Start Service
2. Đợi bài hát phát hết
3. ✅ Nhạc tự động phát lại từ đầu (loop)

## Tính năng MediaPlayer

### Đã implement
- ✅ Phát nhạc từ file local (raw resources)
- ✅ Loop tự động khi hết bài
- ✅ Stop khi service dừng
- ✅ Release resources khi destroy

### MediaPlayer API được dùng
```java
mediaPlayer = MediaPlayer.create(context, R.raw.cung_anh);
mediaPlayer.setLooping(true);      // Loop tự động
mediaPlayer.start();                // Bắt đầu phát
mediaPlayer.stop();                 // Dừng phát
mediaPlayer.isPlaying();            // Kiểm tra đang phát
mediaPlayer.release();              // Giải phóng resources
```

## Logs để Debug

Xem logs trong Android Studio Logcat:

```
D/MusicPlaybackService: Service onCreate
D/MusicPlaybackService: MediaPlayer initialized successfully
D/MusicPlaybackService: Service onStartCommand
D/MusicPlaybackService: Music playback started
D/MusicPlaybackService: Image downloaded successfully
D/MusicPlaybackService: Service onDestroy
D/MusicPlaybackService: MediaPlayer stopped and released
```

## Permissions

Ứng dụng cần các permissions sau (đã có trong AndroidManifest.xml):
- ✅ `INTERNET` - Download ảnh
- ✅ `FOREGROUND_SERVICE` - Chạy service foreground
- ✅ `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Service type
- ✅ `POST_NOTIFICATIONS` - Hiển thị notification (Android 13+)

## Lưu ý

### ⚠️ File nhạc
- File nhạc phải nằm trong `app/src/main/res/raw/`
- Tên file chỉ được dùng chữ thường, số, và gạch dưới
- File không được quá lớn (khuyến nghị < 10MB)

### ⚠️ Memory
- MediaPlayer tự động release khi service destroy
- Bitmap cũng được recycle khi service destroy
- Không lo leak memory

### ⚠️ Battery
- Foreground service tốn pin
- Nên stop service khi không dùng
- Notification giúp user nhớ tắt service

## Troubleshooting

### Không nghe thấy nhạc
**Nguyên nhân**: Volume điện thoại tắt hoặc file nhạc bị lỗi

**Giải pháp**:
1. Kiểm tra volume điện thoại
2. Kiểm tra file `cung_anh.mp3` có phát được không
3. Xem logcat có lỗi MediaPlayer không

### Nhạc phát nhưng không loop
**Nguyên nhân**: `setLooping(true)` không được gọi

**Giải pháp**:
- Kiểm tra code trong `initializeMediaPlayer()`
- Đảm bảo có dòng: `mediaPlayer.setLooping(true);`

### App crash khi start service
**Nguyên nhân**: File nhạc không tồn tại

**Giải pháo**:
- Đảm bảo file `cung_anh.mp3` có trong `res/raw/`
- Build lại project: `.\gradlew clean assembleDebug`

## 🎉 Hoàn thành!

Bây giờ app đã có đầy đủ tính năng:
- ✅ Foreground Service
- ✅ **Phát nhạc thực sự** 🎵
- ✅ Download ảnh từ internet
- ✅ Hiển thị ảnh
- ✅ Notification
- ✅ Start/Stop controls
