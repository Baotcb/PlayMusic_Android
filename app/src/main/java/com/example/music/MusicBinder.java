package com.example.music;

import android.os.Binder;

public class MusicBinder extends Binder {
    private final MusicPlaybackService service;

    public MusicBinder(MusicPlaybackService service) {
        this.service = service;
    }

    public MusicPlaybackService getService() {
        return service;
    }
}
