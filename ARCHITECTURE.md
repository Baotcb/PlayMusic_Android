# Music Playback Service - Architecture Diagram

## Component Interaction Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         MainActivity                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  UI Components:                                           │  │
│  │  • ImageView (displays downloaded image)                  │  │
│  │  • TextView (shows status)                                │  │
│  │  • Start Button                                           │  │
│  │  • Stop Button                                            │  │
│  └───────────────────────────────────────────────────────────┘  │
│                           │          ▲                           │
│                           │          │                           │
│                   1. Start Service   │ 4. Receive Broadcast     │
│                   2. Bind Service    │    & Update UI           │
│                           │          │                           │
└───────────────────────────┼──────────┼───────────────────────────┘
                            │          │
                            ▼          │
┌─────────────────────────────────────────────────────────────────┐
│                   MusicPlaybackService                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Service Lifecycle:                                       │  │
│  │  1. onCreate() - Initialize resources                     │  │
│  │  2. onStartCommand() - Start foreground                   │  │
│  │  3. downloadImage() - Download in background thread       │  │
│  │  4. Send broadcast when complete                          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────┐        ┌───────────────────────────────┐  │
│  │  ExecutorService │───────▶│  Background Thread            │  │
│  │  (Thread Pool)   │        │  • HttpURLConnection          │  │
│  └──────────────────┘        │  • BitmapFactory.decodeStream │  │
│                              └───────────────────────────────┘  │
│                                         │                        │
│                                         ▼                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Handler (Main Thread)                                    │  │
│  │  • Update Notification                                    │  │
│  │  • Send LocalBroadcast                                    │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                   │
│                           │                                      │
└───────────────────────────┼──────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    NotificationManager                           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Foreground Notification:                                 │  │
│  │  • Title: "Music Playing"                                 │  │
│  │  • Text: Shows current status                             │  │
│  │  • Icon: App launcher icon                                │  │
│  │  • Tap action: Opens MainActivity                         │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

```
User Action              Service Action              Network Action
────────────             ──────────────              ──────────────

[Tap Start]
    │
    ├──────────▶ Check Permission
    │               │
    │               ├──────────▶ Request if needed
    │               │
    │               ▼
    ├──────────▶ Start Foreground Service
    │               │
    │               ├──────────▶ Create Notification
    │               │               │
    │               │               ▼
    │               │           [Show in Status Bar]
    │               │
    │               ├──────────▶ ExecutorService.execute()
    │               │               │
    │               │               ├──────────▶ HTTP GET Request
    │               │               │               │
    │               │               │               ├──────────▶ picsum.photos
    │               │               │               │
    │               │               │               ◀──────────┤ Image Data
    │               │               │
    │               │               ├──────────▶ Decode Bitmap
    │               │               │
    │               │               ▼
    │               │           Post to Handler
    │               │               │
    │               ◀───────────────┤
    │               │
    │               ├──────────▶ Update Notification
    │               │
    │               ├──────────▶ Send LocalBroadcast
    │               │
    ◀───────────────┤
    │
[Display Image]
```

## Threading Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Main Thread (UI)                        │
│                                                              │
│  • onCreate() - Initialize views                            │
│  • onClick() - Button handlers                              │
│  • onReceive() - BroadcastReceiver                          │
│  • imageView.setImageBitmap() - Display image               │
│  • statusText.setText() - Update status                     │
│                                                              │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               │ Handler.post()
                               │
                               ▲
┌──────────────────────────────┴──────────────────────────────┐
│                    Background Thread (Network)               │
│                                                              │
│  • URL.openConnection()                                     │
│  • connection.connect()                                     │
│  • InputStream.read()                                       │
│  • BitmapFactory.decodeStream()                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Permission Flow (Android 13+)

```
App Start
   │
   ▼
[User taps Start Service]
   │
   ▼
Check POST_NOTIFICATIONS permission
   │
   ├─────────── Granted ──────────▶ Start Service
   │
   └─────────── Not Granted
                    │
                    ▼
            Request Permission
                    │
                    ├─── User Allows ───▶ Start Service
                    │
                    └─── User Denies ───▶ Show Toast (Permission Denied)
```

## Service Communication Methods

### 1. Binding (Activity → Service)
```java
ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicBinder binder = (MusicBinder) service;
        musicService = binder.getService();
        // Now can access service methods
        Bitmap bitmap = musicService.getDownloadedBitmap();
    }
};
bindService(intent, serviceConnection, BIND_AUTO_CREATE);
```

### 2. Broadcast (Service → Activity)
```java
// Service sends:
Intent intent = new Intent(ACTION_IMAGE_DOWNLOADED);
LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

// Activity receives:
BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Update UI
    }
};
```

## Key Design Patterns Used

1. **Service Pattern**: Background processing independent of Activity lifecycle
2. **Observer Pattern**: BroadcastReceiver for event notification
3. **Binder Pattern**: Direct communication between Activity and Service
4. **Handler Pattern**: Thread synchronization and message passing
5. **Executor Pattern**: Thread pool management for background tasks

