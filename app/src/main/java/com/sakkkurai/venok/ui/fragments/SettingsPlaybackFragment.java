package com.sakkkurai.venok.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.sakkkurai.venok.R;

public class SettingsPlaybackFragment extends Fragment {
    private MaterialSwitch autoPlayOnLaunchSwitch, muteAtZeroVolumeSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings_playback, container, false);
        autoPlayOnLaunchSwitch = v.findViewById(R.id.autolaunchSwitch);
        muteAtZeroVolumeSwitch = v.findViewById(R.id.muteZeroVolumeSwitch);
        SharedPreferences userPrefs = getActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
        SharedPreferences.Editor userPrefsEditor = userPrefs.edit();

        muteAtZeroVolumeSwitch.setChecked(userPrefs.getBoolean("muteAtZeroVolume", true));
        autoPlayOnLaunchSwitch.setChecked(userPrefs.getBoolean("autoplayOnLaunch", false));

        muteAtZeroVolumeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userPrefsEditor.putBoolean("muteAtZeroVolume", isChecked);
                userPrefsEditor.apply();
            }
        });

        autoPlayOnLaunchSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userPrefsEditor.putBoolean("autoplayOnLaunch", isChecked);
                userPrefsEditor.apply();
            }
        });
        return v;
    }
}