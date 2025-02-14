package com.sakkkurai.musicapp.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.adapters.TrackAdapter;
import com.sakkkurai.musicapp.models.Track;
import com.sakkkurai.musicapp.ui.activities.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    RecyclerView trackLibrary;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_home, container, false);
        this.trackLibrary = view.findViewById(R.id.home_musicLibrary);
        trackLibrary.setLayoutManager(new LinearLayoutManager(getContext()));
        trackLibrary.setHasFixedSize(true);
        loadAudioFilesAsync();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        LinearLayoutManager layoutManager = (LinearLayoutManager) trackLibrary.getLayoutManager();
        if (layoutManager != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            SharedPreferences prefs = getActivity().getSharedPreferences("temp", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("LIBRARY_SCROLL_POSITION", position);
            editor.apply();
            Log.d("HomeFragment", "Library scroll position saved at " + position);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getActivity().getSharedPreferences("temp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int scrollPos = prefs.getInt("LIBRARY_SCROLL_POSITION", 0);
        trackLibrary.post(() -> trackLibrary.scrollToPosition(scrollPos));
        editor.remove("LIBRARY_SCROLL_POSITION");
        editor.apply();

        boolean mdedited = prefs.getBoolean("ISMDCHANGED", false);
        if (mdedited)
        {
            editor.putBoolean("ISMDCHANGED", false);
            editor.apply();
            loadAudioFilesAsync();
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private void loadAudioFilesAsync() {

        executor.execute(() -> {
            List<Track> trackList = getAllAudioFilesInBackground();

            mainHandler.post(() -> {
                TrackAdapter trackAdapter = new TrackAdapter(getContext(), new ArrayList<>(trackList), 0, requireActivity());
                trackLibrary.setAdapter(trackAdapter);
                SharedPreferences prefs = getActivity().getSharedPreferences("temp", MODE_PRIVATE);
                int scrollPos = prefs.getInt("LIBRARY_SCROLL_POSITION", 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("LIBRARY_SCROLL_POSITION");
                trackLibrary.post(() -> trackLibrary.scrollToPosition(scrollPos));
            });
        });
    }

    private List<Track> getAllAudioFilesInBackground() {
        List<Track> trackList = new ArrayList<>();
        Uri audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        String selection = MediaStore.Audio.Media.DURATION + ">?";
        SharedPreferences sp = requireActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
        int scanFrom = sp.getInt("userScanFromDuration", -1) * 1000;
        String[] selectionArgs = {String.valueOf(scanFrom)};
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
        };

        ContentResolver contentResolver = getActivity().getContentResolver();
        try (Cursor cursor = contentResolver.query(audioUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

//                    Bitmap albumArt = getAlbumArt(path);
                    trackList.add(new Track(title, artist, album, String.valueOf(duration), path/*, albumArt*/));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("Library", "Error loading audio files: " + e.getMessage());
        }

        return trackList;
    }


}