package com.sakkkurai.venok.services;


import static android.media.AudioAttributes.CONTENT_TYPE_MUSIC;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.MediaStyleNotificationHelper;
import androidx.media3.session.SessionToken;

import com.google.common.collect.ImmutableList;
import com.sakkkurai.venok.R;
import com.sakkkurai.venok.callback.AudioTools;
import com.sakkkurai.venok.database.QueueDatabase;
import com.sakkkurai.venok.database.dao.QueueDao;
import com.sakkkurai.venok.models.Track;
import com.sakkkurai.venok.ui.activities.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicService extends MediaSessionService {

    private ExoPlayer mediaPlayer;
    private MediaSession mediaSession;
    private QueueDatabase qDB;
    private QueueDao queueDao;
    private SharedPreferences queue_prefs, userPrefsPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    private SharedPreferences.Editor queue_editor;
    private final IBinder binder = new MusicServiceBinder();
    private final String TAG = "MusicService";
    public static boolean isRunning = false;
    private AudioManager audioManager;
    private ContentObserver volumeObserver;
    private AudioFocusRequest audioFocusRequest;
    private boolean pausedCauseZeroVolume;
    public class MusicServiceBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    public SessionToken getSessionToken() {
        if (mediaSession == null) {
            Log.w("MusicService", "MediaSession is not initialized yet");
            return null;
        }
        return mediaSession.getToken();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MusicService", "Service started!");
        isRunning = true;
        userPrefsPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "Changed key: " + key);
                if (key.equals("muteAtZeroVolume")) {
                    registerHandlingChangingVolume();
                }
            }
        };

        userPrefsPreferences.registerOnSharedPreferenceChangeListener(listener);

        queue_prefs = getSharedPreferences("queue", MODE_PRIVATE);
        queue_editor = queue_prefs.edit();
        mediaPlayer = new ExoPlayer.Builder(this).build();
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            qDB = QueueDatabase.getInstance(this);
            queueDao = qDB.queueDao();
            Log.d("MusicService", "DB init took: " + (System.currentTimeMillis() - startTime) + "ms");
        }).start();
        if (mediaSession == null) {
            mediaSession = new MediaSession.Builder(this, mediaPlayer)
                    .setCallback(new MediaSession.Callback() {
                    })
                    .setId(getPackageName() + ".MEDIA_SESSION")
                    .build();

        }
        AudioManager.OnAudioFocusChangeListener focusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (mediaPlayer == null) return;

                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.d(TAG, "AUDIOFOCUS_GAIN, isPlaying: " + mediaPlayer.isPlaying() + ", playbackState: " + mediaPlayer.getPlaybackState());
                        mediaPlayer.setVolume(1f);
                        if (!mediaPlayer.isPlaying() && mediaPlayer.getPlaybackState() == Player.STATE_READY) {
                            Log.d(TAG, "Attempting to resume playback");
                            mediaPlayer.play();
                        } else {
                            Log.d(TAG, "Player not ready or already playing, state: " + mediaPlayer.getPlaybackState());
                            if (mediaPlayer.getPlaybackState() == Player.STATE_IDLE || mediaPlayer.getPlaybackState() == Player.STATE_ENDED) {
                                mediaPlayer.prepare();
                                mediaPlayer.play();
                            }
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.d(TAG, "AUDIOFOCUS_LOSS");
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                        mediaPlayer.setVolume(0.2f);
                        break;
                }
            }
        };
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(CONTENT_TYPE_MUSIC)
                        .build())
                .build();
        setMediaNotificationProvider(new notificationProvider(this, mediaSession));
        addSession(mediaSession);
        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                int result;
                if (isPlaying) {
                    Log.d(TAG, "Playback started");
                } else {
                    Log.d(TAG, "Playback paused or stopped");
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                if (mediaItem != null) {
                    queue_editor.putInt("queuePosition", mediaPlayer.getCurrentMediaItemIndex()).apply();
                    Log.d("MusicService", "Queue saved at position: " + mediaPlayer.getCurrentMediaItemIndex());
                }
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        registerHandlingChangingVolume();
        if (intent != null){

            String action = intent.getAction();
            String reason = intent.getStringExtra("reason");

            if ("STOP_SERVICE".equals(action)) {
                Log.d(TAG, "Received STOP_SERVICE action");
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
            Log.d(TAG, "Reason of intent: " + reason);
            if (reason != null) {
                if (reason.equals("START_PLAYBACK_ADAPTER")) {
                    setQueue(true);
                } else if (reason.equals("START_QUIET")) {
                    SharedPreferences preferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
                    if (preferences.getBoolean("autoplayOnLaunch", false)) {
                        setQueue(true);

                    } else {
                        setQueue(false);
                    }
                    startForeground(1, buildStubNotification());
                } else if (reason.equals("STOP_SERVICE")) {
                    stopSelf();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);

    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroying!");
        isRunning = false;
        userPrefsPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
        mediaSession.release();
        mediaPlayer.release();
        if (volumeObserver != null) {
            getContentResolver().unregisterContentObserver(volumeObserver);
            volumeObserver = null;
        }
    }

    @UnstableApi
    private void setQueue(boolean autoplay) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {

            List<Track> trackList = queueDao.getAll();
            ArrayList<Track> tracks = new ArrayList<>(trackList);
            List<MediaItem> queue = new ArrayList<>();
            long startTimeTracks = System.currentTimeMillis();
            for (Track track : tracks) {
                queue.add(trackToMediaItem(track));
            }
            Log.d("MusicService", "Track queue init took: " + (System.currentTimeMillis() - startTimeTracks) + "ms");
            long currentTimeMillis = System.currentTimeMillis();
            mainHandler.post(() -> {
                mediaPlayer.clearMediaItems();
                int result = audioManager.requestAudioFocus(audioFocusRequest);
                if (!queue.isEmpty()) {
                    mediaPlayer.setMediaItems(queue, queue_prefs.getInt("queuePosition", 0), C.TIME_UNSET);
                    Log.d("MusicService", "Set MediaItem's was : " + (System.currentTimeMillis() - currentTimeMillis) + "ms");
                    mediaPlayer.prepare();
                    }
                    if(autoplay) {
                        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                            mediaPlayer.play();
                    }
                    Log.d("MusicService", "MediaItems count: " + mediaPlayer.getMediaItemCount());
                }
            });
        });
    }

    private Notification buildStubNotification() {
        NotificationChannel channel = new NotificationChannel(
                "Music Playback",
                "Music Channel",
                NotificationManager.IMPORTANCE_NONE
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Music Playback")
                .setContentTitle("Music service")
                .setContentText("Starting service...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return builder.build();
    }


    private void registerHandlingChangingVolume() {
        boolean muteAtZeroVolume = userPrefsPreferences.getBoolean("muteAtZeroVolume", true);

        if (volumeObserver != null) {
            getContentResolver().unregisterContentObserver(volumeObserver);
            volumeObserver = null;
        }

        if (!muteAtZeroVolume) return;

        volumeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Log.d(TAG, "Changed volume: " + volume);

                if (volume <= 0) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        pausedCauseZeroVolume = true;
                        Toast.makeText(MusicService.this, R.string.muted_zero_volume, Toast.LENGTH_SHORT).show();
                    }
                } else if (volume > 0 && pausedCauseZeroVolume && !mediaPlayer.isPlaying()) {
                    Toast.makeText(MusicService.this, R.string.unmuted_zero_volume, Toast.LENGTH_SHORT).show();
                    pausedCauseZeroVolume = false;
                    mediaPlayer.play();
                }
            }
        };

        getContentResolver().registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeObserver
        );
    }

    public static MediaItem trackToMediaItem(Track track) {

        return new MediaItem.Builder()
                .setUri(track.getAudioPath())
                .setMediaId(track.getAudioPath())
                .setMediaMetadata(
                        new MediaMetadata.Builder()
                                .setTitle(track.getTrackName())
                                .setArtist(track.getArtistName())
                                .setAlbumTitle(track.getAlbumName())
                                .build()
                )
                .build();
    }
    @UnstableApi
    private class notificationProvider implements MediaNotification.Provider {

        MusicService service;
        MediaSession session;

        public notificationProvider(MusicService service, MediaSession session) {
            this.service = service;
            this.session = session;
        }

        @Override
        public MediaNotification createNotification(
                MediaSession mediaSession,
                ImmutableList<CommandButton> mediaButtonPreferences,
                MediaNotification.ActionFactory actionFactory,
                MediaNotification.Provider.Callback onNotificationChangedCallback) {

            MediaStyleNotificationHelper.MediaStyle mediaStyle = new MediaStyleNotificationHelper.MediaStyle(mediaSession);

            Intent intent = new Intent(service, MainActivity.class);
            intent.putExtra("OPEN_NOWPLAYING", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent contentIntent = PendingIntent.getActivity(
                    service, 0, intent, PendingIntent.FLAG_IMMUTABLE
            );

            Intent dismissIntent = new Intent(service, MusicService.class);
            dismissIntent.setAction("STOP_SERVICE");
            PendingIntent stopIntent = PendingIntent.getService(
                    service, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MusicService.this, "Music Playback")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(mediaSession.getPlayer().getMediaMetadata().title != null
                            ? mediaSession.getPlayer().getMediaMetadata().title
                            : getString(R.string.nowplaying_unknown))
                    .setContentText(mediaSession.getPlayer().getMediaMetadata().artist != null
                            ? mediaSession.getPlayer().getMediaMetadata().artist
                            : getString(R.string.nowplaying_unknown))
                    .setLargeIcon(mediaSession.getPlayer().getMediaMetadata().artworkData != null
                            ? new AudioTools(service).getTrackCover(mediaSession.getPlayer().getMediaMetadata().artworkData)
                            : null)
                    .setStyle(mediaStyle)
                    .setDeleteIntent(stopIntent)
                    .setContentIntent(contentIntent)
                    .setOngoing(mediaSession.getPlayer().isPlaying())
                    .setShowWhen(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            Player player = mediaSession.getPlayer();
            boolean showPauseButton = player.isPlaying() && player.getPlaybackState() != Player.STATE_ENDED;
            ImmutableList<CommandButton> mediaButtons = getMediaButtons(mediaSession, player.getAvailableCommands(), showPauseButton);

            int[] compactViewIndices = addNotificationActions(mediaSession, mediaButtons, builder, actionFactory);

            mediaStyle.setShowActionsInCompactView(compactViewIndices);

            Notification notification = builder.build();
            createNotificationChannel();

            int notificationId = 1;
            return new MediaNotification(notificationId, notification);
        }

        private int[] addNotificationActions(
                MediaSession mediaSession,
                ImmutableList<CommandButton> mediaButtons,
                NotificationCompat.Builder builder,
                MediaNotification.ActionFactory actionFactory) {
            int[] compactViewIndices = new int[3];
            Arrays.fill(compactViewIndices, C.INDEX_UNSET);

            for (int i = 0; i < mediaButtons.size(); i++) {
                CommandButton commandButton = mediaButtons.get(i);
                NotificationCompat.Action action;

                if (commandButton.sessionCommand != null) {
                    action = actionFactory.createCustomActionFromCustomCommandButton(mediaSession, commandButton);
                } else {
                    action = actionFactory.createMediaAction(
                            mediaSession,
                            IconCompat.createWithResource(MusicService.this, commandButton.iconResId),
                            commandButton.displayName,
                            commandButton.playerCommand);
                }

                builder.addAction(action);
                if (commandButton.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                    compactViewIndices[0] = i;
                } else if (commandButton.playerCommand == Player.COMMAND_PLAY_PAUSE) {
                    compactViewIndices[1] = i;
                } else if (commandButton.playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
                    compactViewIndices[2] = i;
                }
            }

            for (int i = 0; i < compactViewIndices.length; i++) {
                if (compactViewIndices[i] == C.INDEX_UNSET) {
                    compactViewIndices = Arrays.copyOf(compactViewIndices, i);
                    break;
                }
            }

            return compactViewIndices;
        }

        private ImmutableList<CommandButton> getMediaButtons(
                MediaSession session,
                Player.Commands playerCommands,
                boolean showPauseButton) {
            ImmutableList.Builder<CommandButton> commandButtons = new ImmutableList.Builder<>();

            if (playerCommands.containsAny(
                    Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
                commandButtons.add(
                        new CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                .setDisplayName(MusicService.this.getString(androidx.media3.session.R.string.media3_controls_seek_to_previous_description))
                                .build());
            }

            if (playerCommands.contains(Player.COMMAND_PLAY_PAUSE)) {
                if (showPauseButton) {
                    commandButtons.add(
                            new CommandButton.Builder(CommandButton.ICON_PAUSE)
                                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                                    .setDisplayName(MusicService.this.getString(androidx.media3.session.R.string.media3_controls_pause_description))
                                    .build());
                } else {
                    commandButtons.add(
                            new CommandButton.Builder(CommandButton.ICON_PLAY)
                                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                                    .setDisplayName(MusicService.this.getString(androidx.media3.session.R.string.media3_controls_play_description))
                                    .build());
                }
            }

            if (playerCommands.containsAny(
                    Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
                commandButtons.add(
                        new CommandButton.Builder(CommandButton.ICON_NEXT)
                                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                .setDisplayName(MusicService.this.getString(androidx.media3.session.R.string.media3_controls_seek_to_next_description))
                                .build());
            }

            return commandButtons.build();
        }



        @Override
        public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
            return false;
        }

        private void createNotificationChannel() {
            NotificationChannel channel = new NotificationChannel(
                    "Music Playback",
                    "Music Channel",
                    NotificationManager.IMPORTANCE_NONE
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
