package com.example.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlaybackService extends Service {
    private static final String TAG = "MusicPlaybackService";
    private static final String CHANNEL_ID = "MusicPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;

    // Sample image URL - you can change this to any image URL
    private static final String IMAGE_URL = "https://picsum.photos/800/600";

    // Sample music URL - you can use any MP3 URL or local resource
    private static final String MUSIC_URL = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";

    public static final String ACTION_IMAGE_DOWNLOADED = "com.example.music.IMAGE_DOWNLOADED";
    public static final String ACTION_DOWNLOAD_STATUS = "com.example.music.DOWNLOAD_STATUS";
    public static final String EXTRA_IMAGE_PATH = "image_path";
    public static final String EXTRA_STATUS = "status";

    private ExecutorService executorService;
    private Handler mainHandler;
    private Bitmap downloadedBitmap;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        initializeMediaPlayer();
    }

    private void initializeMediaPlayer() {
        try {
            // Chỉ sử dụng MediaPlayer.create(), nó đơn giản và hiệu quả
            mediaPlayer = MediaPlayer.create(this, R.raw.cung_anh);

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true); // Tự động lặp lại nhạc

                mediaPlayer.setOnCompletionListener(mp -> {
                    Log.d(TAG, "Music playback completed and will loop.");
                });

                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                    sendStatusBroadcast("Music playback error");
                    return true; // true để chỉ ra rằng lỗi đã được xử lý
                });

                Log.d(TAG, "MediaPlayer initialized successfully from local resource.");
            } else {
                Log.e(TAG, "Failed to create MediaPlayer from resource.");
                sendStatusBroadcast("Failed to initialize music player.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing MediaPlayer", e);
            sendStatusBroadcast("Failed to initialize music player.");
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        // Start the service in foreground with a notification
        Notification notification = createNotification("Starting music playback...");
        startForeground(NOTIFICATION_ID, notification);

        // Start playing music
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d(TAG, "Music playback started");
            updateNotification("Playing music...");
            sendStatusBroadcast("Music playing...");
        }

        // Start downloading image in background
        downloadImage();

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_desc));

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String contentText) {
        Notification notification = createNotification(contentText);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void downloadImage() {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting image download from: " + IMAGE_URL);

                URL url = new URL(IMAGE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();
                downloadedBitmap = BitmapFactory.decodeStream(input);

                input.close();
                connection.disconnect();

                if (downloadedBitmap != null) {
                    Log.d(TAG, "Image downloaded successfully");

                    // Update notification on main thread
                    mainHandler.post(() -> {
                        String musicStatus = (mediaPlayer != null && mediaPlayer.isPlaying())
                                ? "Playing music"
                                : "Music ready";
                        updateNotification("Image downloaded! " + musicStatus);
                        sendStatusBroadcast("Image downloaded successfully. " + musicStatus);
                        sendImageBroadcast();
                    });
                } else {
                    Log.e(TAG, "Failed to decode image");
                    mainHandler.post(() -> {
                        updateNotification("Download failed");
                        sendStatusBroadcast("Download failed");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error downloading image", e);
                mainHandler.post(() -> {
                    updateNotification("Download failed: " + e.getMessage());
                    sendStatusBroadcast("Download failed: " + e.getMessage());
                });
            }
        });
    }

    private void sendImageBroadcast() {
        Intent intent = new Intent(ACTION_IMAGE_DOWNLOADED);
        intent.putExtra(EXTRA_IMAGE_PATH, "bitmap_downloaded");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendStatusBroadcast(String status) {
        Intent intent = new Intent(ACTION_DOWNLOAD_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public Bitmap getDownloadedBitmap() {
        return downloadedBitmap;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");

        // Stop and release MediaPlayer
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer stopped and released");
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        if (downloadedBitmap != null && !downloadedBitmap.isRecycled()) {
            downloadedBitmap.recycle();
            downloadedBitmap = null;
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
}
