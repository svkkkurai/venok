package com.sakkkurai.venok.ui.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.sakkkurai.venok.R;
import com.sakkkurai.venok.ui.fragments.SettingsAppearanceFragment;
import com.sakkkurai.venok.ui.fragments.SettingsPlaybackFragment;

public class SettingsActivity extends AppCompatActivity {
    public static final int REASON_APPEARANCE = 1;
    public static final int REASON_PLAYBACK = 2;
    private final String TAG = "SettingsActivity";
    private FrameLayout settingsFragmentView;
    private MaterialToolbar materialToolbar;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            materialToolbar = findViewById(R.id.settingsTopAppBar);

            materialToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            settingsFragmentView = findViewById(R.id.settings_fragmentview);
            collapsingToolbarLayout = findViewById(R.id.settings_collapsing_toolbar);
            int reason = getIntent().getIntExtra("reason", -1);
            setPage(reason);

        }
    }

    private void setPage(int reason) {
        Log.d(TAG, "Reason: " + String.valueOf(reason));
        switch (reason) {
            case REASON_APPEARANCE:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settings_fragmentview, new SettingsAppearanceFragment()).commit();
                materialToolbar.setTitle(R.string.settings_appearance);
                collapsingToolbarLayout.setTitle(getResources().getString(R.string.settings_appearance));
                break;
            case REASON_PLAYBACK:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.settings_fragmentview, new SettingsPlaybackFragment()).commit();
                materialToolbar.setTitle(R.string.settings_playback);
                collapsingToolbarLayout.setTitle(getResources().getString(R.string.settings_playback));
                break;

        }
    }
}