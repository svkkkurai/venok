package com.sakkkurai.musicapp.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.callback.MetadataManager;
import com.sakkkurai.musicapp.services.MusicService;

public class NowPlayingFragment extends Fragment {
    private String ACTION_NEXT = "ACTION_NEXT";
    private String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    private String ACTION_PAUSE = "ACTION_PAUSE";
    private String ACTION_PLAY = "ACTION_PLAY";
    private TextView songNameTextView, songArtistTextView, songAlbumTextView, songDurationCurrentTextView, songDurationMaxTextView;
    private ImageButton playImageButton, nextImageButton, previousImageButton;
    private boolean isPlaying = false;
    private SharedPreferences preferences;
    private SeekBar songDurationSeekbar;

    private MediaController mediaController;
    private ListenableFuture<MediaController> listenableFuture;
    private final String TAG = "NowPlayingFragment";
    private MusicService musicService;
    private boolean isBound = false;
    private ShapeableImageView songArtworkShapeableImageView;
    private int cornerradius;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) service;
            musicService = binder.getService();
            isBound = true;
            createMediaController();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);
        bindService();
        songNameTextView = view.findViewById(R.id.songName);
        songArtistTextView = view.findViewById(R.id.songArtist);
        songAlbumTextView = view.findViewById(R.id.songAlbum);
        playImageButton = view.findViewById(R.id.nowplaying_controls_play);
        nextImageButton = view.findViewById(R.id.nowplaying_controls_next);
        previousImageButton = view.findViewById(R.id.nowplaying_controls_previous);
        songDurationCurrentTextView = view.findViewById(R.id.nowplaying_playtime_current);
        songDurationMaxTextView = view.findViewById(R.id.nowplaying_playtime_max);
        songArtworkShapeableImageView = view.findViewById(R.id.nowplaying_trackcover);
        songDurationSeekbar = view.findViewById(R.id.nowplaying_trackSeekbar);
        preferences = requireContext().getSharedPreferences("userPrefs", MODE_PRIVATE);
        cornerradius = preferences.getInt("cornerradius", R.integer.cornerradius);

        // Button listeners
        playImageButton.setOnClickListener(v -> handleControls(ACTION_PLAY));
        nextImageButton.setOnClickListener(v -> handleControls(ACTION_NEXT));
        previousImageButton.setOnClickListener(v -> handleControls(ACTION_PREVIOUS));
        songDurationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mediaController.seekTo(songDurationSeekbar.getProgress());
            }
        });
        return view;
    }

    private void createMediaController(){
        listenableFuture = new MediaController.Builder(requireContext(), musicService.getSessionToken()).buildAsync();
        listenableFuture.addListener(() -> {
            try {
                mediaController = listenableFuture.get();
                Log.d(TAG, "MediaController initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize MediaController", e);
            }
        }, MoreExecutors.directExecutor());
        if (mediaController == null){
            Log.d(TAG, "MediaController is null!");
            return;
        }
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                updateCurrentPlayingPosition();
                updateUI();
            }

            @Override
            public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
                Player.Listener.super.onMediaMetadataChanged(mediaMetadata);
                updateCurrentPlayingPosition();
                updateUI();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                updateUI();
                updateCurrentPlayingPosition();
                Log.d(TAG, "CurrentPos: " + mediaController.getCurrentPosition());
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                updateUI();
                updateCurrentPlayingPosition();
            }
        });
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentPlayingPosition();
                handler.postDelayed(this, 250);
            }
        };
        handler.post(runnable);
        updateUI();

    }

    private void updateCurrentPlayingPosition() {
        songDurationSeekbar.setProgress((int) mediaController.getCurrentPosition());
        songDurationCurrentTextView.setText(new MetadataManager(getActivity()).getFormattedDuration(mediaController.getCurrentPosition()));
    }
    private void updateUI(){
        isPlaying = mediaController.isPlaying();
        if (isPlaying) {
            playImageButton.setImageResource(R.drawable.nowplaying_pause);
        }
        if (!isPlaying) {
            playImageButton.setImageResource(R.drawable.nowplaying_play);
        }
        if (isAdded()) {
            MetadataManager metadataManager = new MetadataManager(getActivity());
            long songDurationMs = mediaController.getDuration();
            songDurationMaxTextView.setText(metadataManager.getFormattedDuration(songDurationMs));
            songDurationSeekbar.setMax((int) songDurationMs);
        }


        songNameTextView.setText(mediaController.getMediaMetadata().title);
        songArtistTextView.setText(mediaController.getMediaMetadata().artist);
        songAlbumTextView.setText(mediaController.getMediaMetadata().albumTitle);
        byte[] artworkData = mediaController.getMediaMetadata().artworkData;

        if (artworkData != null && artworkData.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.length);
            if (bitmap != null) {
                if (isAdded()) {
                    RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(requireContext().getResources(), bitmap);
                    rbd.setCornerRadius(cornerradius);
                    songArtworkShapeableImageView.setImageDrawable(rbd);
                    Log.d(TAG, "updateUI: Album art set successfully");
                }
            } else {
                songArtworkShapeableImageView.setImageDrawable(null);
                Log.w(TAG, "updateUI: Failed to decode album art bitmap");
            }
        } else {
            songArtworkShapeableImageView.setImageDrawable(null);
            Log.d(TAG, "updateUI: No album art available");
        }
        Log.d(TAG, "isPlaying: " + isPlaying + "\nTitle: " + songNameTextView.getText() + ", Artist: " + songArtistTextView.getText() +
                ", Album: " + songAlbumTextView.getText());
    }
    private void bindService(){
        Log.d(TAG, "bindService: Attempting to bind to MusicService");
        Intent intent = new Intent(requireContext(), MusicService.class);
        intent.putExtra("reason", "START_QUIET");
        boolean bindResult = requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "bindService: Bind result: " + bindResult);
    }

    private void handleControls(String action) {
    if (mediaController == null) {
        Log.d(TAG, "MediaController is null!");
        return;
    }
        if (action.equals(ACTION_PLAY)) {
            if (isPlaying) {
                mediaController.pause();
                isPlaying = false;
            } else {
                mediaController.play();
                isPlaying = true;
            }
        } else if (action.equals(ACTION_PAUSE)) {
            mediaController.pause();
            isPlaying = false;
        } else if (action.equals(ACTION_NEXT)) {
            mediaController.seekToNext();
        } else if (action.equals(ACTION_PREVIOUS)) {
            mediaController.seekToPrevious();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireContext().unbindService(connection);
            isBound = false;
        }
    }
}