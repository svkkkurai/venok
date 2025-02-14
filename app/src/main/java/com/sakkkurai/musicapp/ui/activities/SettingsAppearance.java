package com.sakkkurai.musicapp.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.sakkkurai.musicapp.R;

public class SettingsAppearance extends AppCompatActivity {

    private SeekBar np_round_sb;
    private TextView np_current_round;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_appearance);

        MaterialToolbar appbar = findViewById(R.id.topAppBar);

        appbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        np_current_round = findViewById(R.id.appearance_np_cornerradius_now);
        np_round_sb = findViewById(R.id.appearance_np_cornerradius_seekbar);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        int np_savedround = prefs.getInt("appearance_nowplaying_trackCoverRound", 0);

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
                SharedPreferences sp = getSharedPreferences("userPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt("appearance_nowplaying_trackCoverRound", np_round_sb.getProgress());
                editor.apply();

                Log.d("Prefs", "Saved! Cover corner radius " + String.valueOf(np_round_sb.getProgress()));
            }
        });
    }
}