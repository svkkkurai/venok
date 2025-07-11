package com.sakkkurai.venok.ui.fragments;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sakkkurai.venok.R;
import com.sakkkurai.venok.adapters.ArtistAdapter;
import com.sakkkurai.venok.models.Track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtistFragment extends Fragment {
    private RecyclerView artistLibrary;
    private String TAG = "ArtistFragment";

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_artist, container, false);

        artistLibrary = v.findViewById(R.id.artist_artistLibrary);
        artistLibrary.setLayoutManager(new LinearLayoutManager(getContext()));
        setArtistLibrary();
        return v;
    }


    private void setArtistLibrary() {
        List<String> artists = new ArrayList<>();
        String sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";

        String[] projection = {
                MediaStore.Audio.Media.ARTIST,
        };

        Uri audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getActivity().getContentResolver();
        try (Cursor cursor = contentResolver.query(audioUri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    artists.add(artist);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("Library", "Error loading artists: " + e.getMessage());
        }
        Set<String> set = new HashSet<>(artists);
        artists.clear();
        artists.addAll(set);
        ArtistAdapter artistAdapter = new ArtistAdapter(getActivity(), new ArrayList<>(artists));
        artistLibrary.setAdapter(artistAdapter);
        Log.d(TAG, String.valueOf(artists));


    }
}