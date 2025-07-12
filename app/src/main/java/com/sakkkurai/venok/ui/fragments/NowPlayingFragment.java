package com.sakkkurai.venok.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.sakkkurai.venok.R;
import com.sakkkurai.venok.tools.AudioTools;
import com.sakkkurai.venok.services.MusicService;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NowPlayingFragment extends Fragment {
    private String ACTION_NEXT = "ACTION_NEXT";
    private String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    private String ACTION_PAUSE = "ACTION_PAUSE";
    private String ACTION_PLAY = "ACTION_PLAY";
    private TextView songNameTextView, songArtistTextView, songAlbumTextView, songDurationCurrentTextView, songDurationMaxTextView;
    private ImageButton playImageButton, nextImageButton, previousImageButton;
    private boolean isPlaying = false;
    private SharedPreferences preferences;
    private Slider songDurationSeekbar;
    private MediaController mediaController;
    private boolean isTracking;
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
        cornerradius = preferences.getInt("cornerradius", requireContext().getResources().getInteger(R.integer.cornerradius));

        // Button listeners
        playImageButton.setOnClickListener(v -> handleControls(ACTION_PLAY));
        nextImageButton.setOnClickListener(v -> handleControls(ACTION_NEXT));
        previousImageButton.setOnClickListener(v -> handleControls(ACTION_PREVIOUS));



        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Log.d(TAG, "Changed key: " + key);
                if (key.equals("cornerradius")) {
                    cornerradius = prefs.getInt(key, 16);
                    updateUI(true);
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(listener);


        songDurationSeekbar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                slider.getValue();
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                isTracking = false;
                mediaController.seekTo((long) songDurationSeekbar.getValue());
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
                updateUI(true);
            }

            @Override
            public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
                Player.Listener.super.onMediaMetadataChanged(mediaMetadata);
                updateCurrentPlayingPosition();
                updateUI(true);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                updateUI(false);
                updateCurrentPlayingPosition();
                Log.d(TAG, "CurrentPos: " + mediaController.getCurrentPosition());
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                updateUI(false);
                updateCurrentPlayingPosition();
            }
        });
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentPlayingPosition();
                handler.postDelayed(this, 50);
            }
        };
        handler.post(runnable);
        updateUI(true);

    }

    private void updateCurrentPlayingPosition() {
        if (mediaController.getPlaybackState() == Player.STATE_READY) {
            if (!isTracking) {
                songDurationSeekbar.setValue((int) mediaController.getCurrentPosition());
            }
            songDurationCurrentTextView.setText(new AudioTools(getActivity()).getFormattedDuration(mediaController.getCurrentPosition()));
        }

    }
    private void updateUI(boolean shouldUpdateArtwork){
        MediaMetadata metadata = mediaController.getMediaMetadata();
        if (metadata == null) return;

        String name = (String) metadata.title;
        String artist = (String) metadata.artist;
        String album = (String) metadata.albumTitle;

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(artist) && TextUtils.isEmpty(album)) {
            return;
        }


        songNameTextView.setText(name);
        songArtistTextView.setText(artist);
        songAlbumTextView.setText(album);
        isPlaying = mediaController.isPlaying();

        // ClickableSpan
        String artistSpan = songArtistTextView.getText().toString();
        String[] artists_span = artistSpan.split(",\\s*");
        Log.d(TAG, Arrays.toString(artists_span));
        SpannableString spannable = new SpannableString(artistSpan);
        int start = 0;
        for (String part : artists_span) {
            int end = start + part.length();
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    int originalTextColor = songArtistTextView.getCurrentTextColor();
                    ds.setColor(originalTextColor);
                    ds.setUnderlineText(false);
                }

                @Override
                public void onClick(@NonNull View widget) {
                    Toast.makeText(widget.getContext(), "Clicked: " + part, Toast.LENGTH_SHORT).show();
                }
            };



            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = end + 2;
        }
        songArtistTextView.setText(spannable);
        songArtistTextView.setHighlightColor(Color.TRANSPARENT);
        songArtistTextView.setMovementMethod(LinkMovementMethod.getInstance());




        if (shouldUpdateArtwork) {
            byte[] artworkData = mediaController.getMediaMetadata().artworkData;
            ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executorService.execute(()-> {
                handler.post(()-> {
                    if (artworkData != null && artworkData.length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.length);
                        if (bitmap != null) {
                            if (isAdded()) {
                                RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(requireContext().getResources(), bitmap);
                                rbd.setCornerRadius(cornerradius);
                                songArtworkShapeableImageView.setAlpha(0f);
                                songArtworkShapeableImageView.setImageDrawable(rbd);
                                songArtworkShapeableImageView.animate()
                                        .alpha(1f)
                                        .setDuration(300)
                                        .start();
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
                });
            });
        }

        if (isAdded()) {
            if (mediaController.getPlaybackState() == Player.STATE_READY) {
                AudioTools audioTools = new AudioTools(getActivity());
                long songDurationMs = mediaController.getDuration();
                songDurationMaxTextView.setText(audioTools.getFormattedDuration(songDurationMs));
                songDurationSeekbar.setValueTo((int) songDurationMs);
            }
        }



        Log.d(TAG, "isPlaying: " + isPlaying + "\nTitle: " + songNameTextView.getText() + ", Artist: " + songArtistTextView.getText() +
                ", Album: " + songAlbumTextView.getText());

        if(mediaController.getPlaybackState() != Player.STATE_BUFFERING) {
            if (isPlaying) {
                playImageButton.setImageResource(R.drawable.nowplaying_pause);
            }
            if (!isPlaying) {
                playImageButton.setImageResource(R.drawable.nowplaying_play);
            }
        }
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