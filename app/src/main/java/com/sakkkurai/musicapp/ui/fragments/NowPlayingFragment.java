package com.sakkkurai.musicapp.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.callback.MetadataManager;
import com.sakkkurai.musicapp.models.Track;
import com.sakkkurai.musicapp.services.MusicService;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class NowPlayingFragment extends Fragment {

    private TextView songName, songArtist, songAlbum, playingFrom, playingFromTitle, seekbar_currentTime, seekbar_maxTime, trackLyrics;
    private ShapeableImageView songCover;
    private ImageButton pause, next, previous;
    int np_savedround;
    private SeekBar seekBar;
    private boolean isPlaying;
    private Track trackfrompos;
    private int pos;
    private long trackDurationCurrentPos;
    private ArrayList<Track> queuefromsp;
    private Drawable playDrawable;
    private Drawable pauseDrawable;



    private final BroadcastReceiver trackChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateData();
        }
    };


    private final BroadcastReceiver trackDurationCurrentPosition = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            trackDurationCurrentPos = intent.getLongExtra("POSITION", 0);
            seekBar.setProgress((int) trackDurationCurrentPos);
            if (intent.hasExtra("isPlaying")) {
                isPlaying = intent.getBooleanExtra("isPlaying", false);
                if (isPlaying && pause.getDrawable() != pauseDrawable) {
                    pause.setImageDrawable(pauseDrawable);
                } else if (!isPlaying && pause.getDrawable() != playDrawable) {
                    pause.setImageDrawable(playDrawable);
                }
            }
        }
    };



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_now_playing, container, false);
        playDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.nowplaying_play);
        pauseDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.nowplaying_pause);
        songCover = v.findViewById(R.id.nowplaying_trackcover);
        songName = v.findViewById(R.id.songName);
        songArtist = v.findViewById(R.id.songArtist);
        trackLyrics = v.findViewById(R.id.nowplaying_trackLyrics);
        next = v.findViewById(R.id.nowplaying_controls_next);
        pause = v.findViewById(R.id.nowplaying_controls_play);
        previous = v.findViewById(R.id.nowplaying_controls_previous);
        songAlbum = v.findViewById(R.id.songAlbum);
        playingFrom = v.findViewById(R.id.nowplaying_playingfrom);
        playingFromTitle = v.findViewById(R.id.nowplaying_playingfrom_title);
        seekbar_maxTime = v.findViewById(R.id.nowplaying_playtime_max);
        seekbar_currentTime = v.findViewById(R.id.nowplaying_playtime_current);
        seekBar = v.findViewById(R.id.nowplaying_playtime_bar);

        SharedPreferences prefs = requireActivity().getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        np_savedround = prefs.getInt("appearance_nowplaying_trackCoverRound", 16);


        Intent updateData = new Intent(getActivity(), MusicService.class);
        updateData.setAction("NOWPLAYING_INIT");
        ContextCompat.startForegroundService(getActivity(), updateData);

        IntentFilter trackDur = new IntentFilter("com.sakkkurai.musicapp.TRACK_CURRENTPOS");
        ContextCompat.registerReceiver(requireActivity(), trackDurationCurrentPosition, trackDur, ContextCompat.RECEIVER_EXPORTED);

        IntentFilter trackChanged = new IntentFilter("com.sakkkurai.musicapp.TRACK_CHANGED");
        ContextCompat.registerReceiver(requireActivity(), trackChangeReceiver, trackChanged, ContextCompat.RECEIVER_EXPORTED);


        updateData();


        pause.setOnClickListener(v1 -> {
            if (isPlaying) {
                Intent playIntent = new Intent(getActivity(), MusicService.class);
                playIntent.setAction("ACTION_PAUSE");
                ContextCompat.startForegroundService(getActivity(), playIntent);
                pause.setImageResource(R.drawable.nowplaying_play);
            } else {
                Intent playIntent = new Intent(getActivity(), MusicService.class);
                playIntent.setAction("ACTION_PLAY");
                ContextCompat.startForegroundService(getActivity(), playIntent);
                pause.setImageResource(R.drawable.nowplaying_pause);
            }
            isPlaying = !isPlaying;
            updateData();
        });

        next.setOnClickListener(v1 -> {
            if (pos != queuefromsp.size() -1) {
                SharedPreferences preferences = getActivity().getSharedPreferences("queue", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("queuePosition", pos + 1);
                editor.apply();
                updateData();
                Intent playIntent = new Intent(getActivity(), MusicService.class);
                playIntent.setAction("ACTION_PLAY_TRACK_LIBRARY");
                ContextCompat.startForegroundService(getActivity(), playIntent);
                pause.setImageResource(R.drawable.nowplaying_pause);
                seekBar.setProgress(0);
            } else {
                Toast.makeText(getActivity(), R.string.nowplaying_queue_reachedendlimit, Toast.LENGTH_SHORT).show();
                pause.setImageResource(R.drawable.nowplaying_play);

            }
        });


        previous.setOnClickListener(v1 -> {
            if (pos != 0) {
                if (seekBar.getProgress() < 2000) {
                    SharedPreferences preferences = getActivity().getSharedPreferences("queue", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("queuePosition", pos - 1);
                    editor.apply();
                    updateData();
                    Intent playIntent = new Intent(getActivity(), MusicService.class);
                    playIntent.setAction("ACTION_PLAY_TRACK_LIBRARY");
                    ContextCompat.startForegroundService(getActivity(), playIntent);
                    pause.setImageResource(R.drawable.nowplaying_pause);
                    seekBar.setProgress(0);
                } else {
                    Intent playIntent = new Intent(getActivity(), MusicService.class);
                    playIntent.setAction("ACTION_PLAY_TRACK_LIBRARY");
                    ContextCompat.startForegroundService(getActivity(), playIntent);
                }

            } else {
                Toast.makeText(getActivity(), R.string.nowplaying_queue_reachedstartlimit, Toast.LENGTH_SHORT).show();
                pause.setImageResource(R.drawable.nowplaying_play);
            }

        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                long totalSeconds = progress / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                long hours = minutes / 60;
                minutes = minutes % 60;

                if (totalSeconds < 600) { // < 10 min
                    seekbar_currentTime.setText(String.format("%d:%02d", minutes, seconds));
                } else if (totalSeconds < 3600) { // >10 min and <60 min
                    seekbar_currentTime.setText(String.format("%02d:%02d", minutes, seconds));
                } else if (totalSeconds < 36000) { // >60 min and <600 min
                    seekbar_currentTime.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
                } else { // >600 min and <1440 min
                    seekbar_currentTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Intent playIntent = new Intent(getActivity(), MusicService.class);
                playIntent.setAction("REWIND");
                playIntent.putExtra("SEEK_TO", seekBar.getProgress());
                ContextCompat.startForegroundService(getActivity(), playIntent);
            }
        });
        return v;
    }


    public void updateData() {
        trackLyrics.setVisibility(View.GONE);
        SharedPreferences preferences = getActivity().getSharedPreferences("queue", MODE_PRIVATE);
        Gson gson = new Gson();
        if (isPlaying) {
            pause.setImageResource(R.drawable.nowplaying_pause);
        }
        else {
            pause.setImageResource(R.drawable.nowplaying_play);
        }
        if (preferences.contains("queue") && preferences.contains("queuePosition")) {
            String playReason = preferences.getString("playReason", "");
            String queue = preferences.getString("queue", "");
            pos = preferences.getInt("queuePosition", 0);
            Type type = new TypeToken<ArrayList<Track>>() {}.getType();
            queuefromsp = gson.fromJson(queue, type);
            trackfrompos = queuefromsp.get(pos);
            MetadataManager mdm = new MetadataManager(getActivity());
            songName.setText(trackfrompos.getTrackName());
            songArtist.setText(trackfrompos.getArtistName());
            songAlbum.setText(trackfrompos.getAlbumName());
            seekBar.setMax(trackfrompos.getDurationMs());
            seekbar_maxTime.setText(trackfrompos.getDuration());
            Bitmap songImage = mdm.getTrackCover(trackfrompos.getAudioPath());
            if (songImage != null) {
                RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(getResources(), songImage);
                rbd.setCornerRadius(np_savedround);
                songCover.setImageDrawable(rbd);
            } else {
                songCover.setImageDrawable(null);
            }
        } else {
            songCover.setImageDrawable(null);
            songName.setText(R.string.nowplaying_emptyqueue);
            songArtist.setText(R.string.nowplaying_unknown);
            songAlbum.setText(R.string.nowplaying_unknown);
            playingFromTitle.setVisibility(View.INVISIBLE);
            playingFrom.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().unregisterReceiver(trackChangeReceiver);
        requireActivity().unregisterReceiver(trackDurationCurrentPosition);
    }
}