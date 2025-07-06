package com.sakkkurai.venok.ui.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sakkkurai.venok.R;
import com.sakkkurai.venok.adapters.ArtistAdapter;

import java.util.ArrayList;
import java.util.List;

public class ArtistFragment extends Fragment {
    private RecyclerView artistLibrary;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_artist, container, false);

        artistLibrary = v.findViewById(R.id.artist_artistLibrary);
        createArtistList();
        artistLibrary.setLayoutManager(new LinearLayoutManager(getContext()));
        return v;
    }

    private void createArtistList() {
        List<String> list = new ArrayList<>();
        list.add("sex");
        list.add("hitler");
        list.add("eblan");
        ArtistAdapter artistAdapter = new ArtistAdapter(getActivity(), new ArrayList<>(list));
        artistLibrary.setAdapter(artistAdapter);
    }
}