package com.sakkkurai.venok.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sakkkurai.venok.R;

public class SettingsAppearanceFragment extends Fragment {

    private SeekBar np_round_sb;
    private TextView np_current_round;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings_appearance, container, false);
        /*MaterialToolbar appbar = v.findViewById(R.id.topAppBar);

        appbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });*/

        np_current_round = v.findViewById(R.id.appearance_np_cornerradius_now);
        np_round_sb = v.findViewById(R.id.appearance_np_cornerradius_seekbar);

        SharedPreferences prefs = getActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
        int np_savedround = prefs.getInt("cornerradius", getResources().getInteger(R.integer.cornerradius));

        np_round_sb.setProgress(np_savedround);
        np_current_round.setText(String.valueOf(np_savedround));


        np_round_sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                np_current_round.setText(String.valueOf(np_round_sb.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save round
                SharedPreferences sp = getActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("cornerradius", np_round_sb.getProgress());
                editor.apply();

                Log.d("Prefs", "Saved! Cover corner radius " + String.valueOf(np_round_sb.getProgress()));
            }
        });
        return v;
    }
}