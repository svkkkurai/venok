package com.sakkkurai.musicapp.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.common.util.NotificationUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
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
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.callback.MetadataManager;
import com.sakkkurai.musicapp.database.QueueDatabase;
import com.sakkkurai.musicapp.database.dao.QueueDao;
import com.sakkkurai.musicapp.models.Track;
import com.sakkkurai.musicapp.ui.activities.MainActivity;

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
    private SharedPreferences queue_prefs;
    private SharedPreferences.Editor queue_editor;
    private final IBinder binder = new MusicServiceBinder();
    private String TAG = "MusicService";

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

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MusicService", "Service started!");

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
                    .setId(getPackageName() + ".MEDIA_SESSION")
                    .build();
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("OPEN_NOWPLAYING", true);
            PendingIntent pi = PendingIntent.getActivity(MusicService.this, 12, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT );
            mediaSession.setSessionActivity(pi);
        }
        setMediaNotificationProvider(new notificationProvider(this, mediaSession));
        addSession(mediaSession);


        mediaPlayer.addListener(new Player.Listener() {
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
        if (intent != null){
            String reason = intent.getStringExtra("reason");
            Log.d(TAG, "Reason of intent: " + reason);
            if (reason != null) {
                if (reason.equals("START_PLAYBACK_ADAPTER")) {
                    setQueue(true);
                } else if (reason.equals("START_QUIET")) {
                    setQueue(false);
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
        mediaSession.release();
        mediaPlayer.release();
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
                if (!queue.isEmpty()) {
                    mediaPlayer.setMediaItems(queue, queue_prefs.getInt("queuePosition", 0), C.TIME_UNSET);
                    Log.d("MusicService", "Set MediaItem's was : " + (System.currentTimeMillis() - currentTimeMillis) + "ms");
                    mediaPlayer.prepare();
                    if(autoplay) {
                        mediaPlayer.play();
                    }
                    Log.d("MusicService", "MediaItems count: " + mediaPlayer.getMediaItemCount());
                }
            });
        });
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

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MusicService.this, "Music Playback")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(mediaSession.getPlayer().getMediaMetadata().title != null
                            ? mediaSession.getPlayer().getMediaMetadata().title
                            : getString(R.string.nowplaying_unknown))
                    .setContentText(mediaSession.getPlayer().getMediaMetadata().artist != null
                            ? mediaSession.getPlayer().getMediaMetadata().artist
                            : getString(R.string.nowplaying_unknown))
                    .setLargeIcon(mediaSession.getPlayer().getMediaMetadata().artworkData != null
                            ? new MetadataManager(service).getTrackCover(mediaSession.getPlayer().getMediaMetadata().artworkData)
                            : null)
                    .setStyle(mediaStyle)
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
