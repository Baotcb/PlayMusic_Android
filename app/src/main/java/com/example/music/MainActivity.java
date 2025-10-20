package com.example.music;

import static androidx.core.content.ContextCompat.startForegroundService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView statusText;
    private Button btnStart;
    private Button btnStop;
    
    private MusicPlaybackService musicService;
    private boolean serviceBound = false;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startMusicService();
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            
            // Check if image is already downloaded
            Bitmap bitmap = musicService.getDownloadedBitmap();
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicPlaybackService.ACTION_IMAGE_DOWNLOADED.equals(intent.getAction())) {
                if (serviceBound && musicService != null) {
                    Bitmap bitmap = musicService.getDownloadedBitmap();
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        statusText.setText(R.string.image_downloaded);
                    }
                }
            } else if (MusicPlaybackService.ACTION_DOWNLOAD_STATUS.equals(intent.getAction())) {
                String status = intent.getStringExtra(MusicPlaybackService.EXTRA_STATUS);
                if (status != null) {
                    statusText.setText(status);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        initViews();
        setupListeners();
        registerReceivers();
    }

    private void initViews() {
        imageView = findViewById(R.id.imageView);
        statusText = findViewById(R.id.statusText);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> {
            checkPermissionAndStartService();
        });

        btnStop.setOnClickListener(v -> {
            stopMusicService();
        });
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlaybackService.ACTION_IMAGE_DOWNLOADED);
        filter.addAction(MusicPlaybackService.ACTION_DOWNLOAD_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(downloadReceiver, filter);
    }

    private void checkPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startMusicService();
            }
        } else {
            startMusicService();
        }
    }

    private void startMusicService() {
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Bind to the service to get the downloaded bitmap
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        statusText.setText(R.string.downloading_image);
        Toast.makeText(this, "Music service started", Toast.LENGTH_SHORT).show();
    }

    private void stopMusicService() {
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        stopService(serviceIntent);
        
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        
        statusText.setText(R.string.status_ready);
        imageView.setImageBitmap(null);
        Toast.makeText(this, "Music service stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(downloadReceiver);
        
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
