package com.sakkkurai.musicapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.callback.MetadataManager;
import com.sakkkurai.musicapp.models.Track;
import com.sakkkurai.musicapp.ui.activities.MainActivity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MusicService extends Service {
    private ExoPlayer mediaPlayer;
    private MediaItem mediaItem;
    private MediaSession mediaSession;
    private boolean isPlaying, cancelableNotification;
    private int pos;
    private Track trackfrompos;
    private ArrayList<Track> queuefromsp;
    private List<MediaItem> mediaItems;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateData();
        createNotificationChannel();
        String sessionId = UUID.randomUUID().toString();
        handler.post(broadcastRunnable);
        }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_NEXT":
                    nextTrack();
                    break;
                case "ACTION_PREV":
                    previousTrack();
                    break;
                case "ACTION_PLAY":
                    play();
                    break;
                case "ACTION_PAUSE":
                    pause();
                    break;
                case "ACTION_UPDATE_DATA":
                    updateData();
                    break;
                case "ACTION_REPLAY":
                    mediaPlayer.seekTo(0);
                    break;
                case "ACTION_PLAY_TRACK_LIBRARY":
                    Intent intent2 = new Intent("com.sakkkurai.musicapp.TRACK_CHANGED");
                    intent2.setPackage(getPackageName());
                    sendBroadcast(intent2);
                    updateData();
                    play();
                    break;
                case "NOWPLAYING_INIT":
                    NPInit();
                    break;
                case "REWIND":
                    mediaPlayer.seekTo(intent.getIntExtra("SEEK_TO", 0));
                    break;
            }
        }
        return START_STICKY;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable broadcastRunnable = new Runnable() {
        @Override
        public void run() {
            NPInit();
            handler.postDelayed(this, 1000);
        }
    };

    private void NPInit() {
        Intent intent = new Intent("com.sakkkurai.musicapp.TRACK_CURRENTPOS");
        intent.putExtra("POSITION", mediaPlayer.getCurrentPosition());
        intent.putExtra("isPlaying", mediaPlayer.isPlaying());
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void updateData() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        Gson gson = new Gson();
        SharedPreferences prefs = getSharedPreferences("queue", MODE_PRIVATE);
        if (prefs.contains("queue") && prefs.contains("queuePosition")) {
            String queue = prefs.getString("queue", "");
            pos = prefs.getInt("queuePosition", 0);
            Type type = new TypeToken<ArrayList<Track>>() {}.getType();
            queuefromsp = gson.fromJson(queue, type);
            trackfrompos = queuefromsp.get(pos);
            Uri uri = Uri.parse("file://" + trackfrompos.getAudioPath());
            mediaItems = new ArrayList<>();

            for (Track track : queuefromsp) {
                MediaItem item = new MediaItem.Builder()
                        .setUri(Uri.parse("file://" + track.getAudioPath()))
                        .build();
                mediaItems.add(item);
            }
            mediaPlayer = new ExoPlayer.Builder(this).build();
            mediaPlayer.setMediaItems(mediaItems);
            mediaPlayer.seekTo(pos, 0L);
            mediaPlayer.prepare();
            isPlaying = false;
            String sessionId = UUID.randomUUID().toString();
            mediaSession = new MediaSession.Builder(this, mediaPlayer)
                    .setId(sessionId)
                    .build();
            mediaPlayer.addListener(new Player.Listener() {


                @Override
                public void onPlaybackStateChanged(int state) {
                    saveTrackPos();
                    trackChanged();
                    if (state == Player.STATE_ENDED) {
                        nextTrack();
                        saveTrackPos();
                        trackChanged();
                    }

                }
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    saveTrackPos();
                    trackChanged();
                }

            });
            try {
                startForeground(1, buildNotification());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void saveTrackPos() {
        SharedPreferences preferences = this.getSharedPreferences("queue", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("queuePosition", mediaPlayer.getCurrentMediaItemIndex());
        editor.apply();
    }
    private void pause() {
        isPlaying = false;
        cancelableNotification = true;
        mediaPlayer.pause();
    }

    private void play() {
        isPlaying = true;
        cancelableNotification = false;
        mediaPlayer.play();
    }

private void trackChanged() {
    Intent intent = new Intent("com.sakkkurai.musicapp.TRACK_CHANGED");
    intent.setPackage(getPackageName());
    sendBroadcast(intent);
}
    private void nextTrack() {
        if (pos != queuefromsp.size() - 1) {
            SharedPreferences preferences = this.getSharedPreferences("queue", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("queuePosition", pos + 1);
            editor.apply();

            Intent playIntent = new Intent(this, MusicService.class);
            playIntent.setAction("ACTION_PLAY_TRACK_LIBRARY");
            ContextCompat.startForegroundService(this, playIntent);
            trackChanged();
            createNotificationChannel();

        } else {
            Toast.makeText(this, R.string.nowplaying_queue_reachedendlimit, Toast.LENGTH_SHORT).show();
        }
    }

    private void previousTrack() {
        if (pos != queuefromsp.size() - 1) {
            if (mediaPlayer.getCurrentPosition() < 5000) {
                SharedPreferences preferences = getSharedPreferences("queue", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("queuePosition", pos - 1);
                editor.apply();
                updateData();
                Intent playIntent = new Intent(this, MusicService.class);
                playIntent.setAction("ACTION_PLAY_TRACK_LIBRARY");
                ContextCompat.startForegroundService(this, playIntent);
                mediaPlayer.seekTo(0);
                createNotificationChannel();
            } else {
                Intent playIntent = new Intent(this, MusicService.class);
                playIntent.setAction("ACTION_PLAY_TRACK_LIBRARY");
                ContextCompat.startForegroundService(this, playIntent);
            }
        } else {
            Toast.makeText(this, R.string.nowplaying_queue_reachedendlimit, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(broadcastRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "Music Playback",
                "Music Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @OptIn(markerClass = UnstableApi.class)
    private Notification buildNotification() throws IOException {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "Music Playback")
                .setContentTitle("Track Title")
                .setContentText("Artist Name")
                .setSmallIcon(R.drawable.music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(cancelableNotification)
                .setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(trackfrompos.getBitmap())
                .setContentTitle(trackfrompos.getTrackName())
                .setContentText(trackfrompos.getArtistName())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        PendingIntent prevIntent = PendingIntent.getService(
                this, 0, new Intent(this, MusicService.class).setAction("ACTION_PREV"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent nextIntent = PendingIntent.getService(
                this, 0, new Intent(this, MusicService.class).setAction("ACTION_NEXT"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return notification;
}
}

