package com.sakkkurai.musicapp.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sakkkurai.musicapp.services.MusicService;
import com.sakkkurai.musicapp.ui.fragments.ArtistFragment;
import com.sakkkurai.musicapp.ui.fragments.HomeFragment;
import com.sakkkurai.musicapp.ui.fragments.NowPlayingFragment;
import com.sakkkurai.musicapp.ui.fragments.PlaylistFragment;
import com.sakkkurai.musicapp.R;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.sakkkurai.musicapp.ui.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    int READ_AUDIO_PERMISSION_CODE = 1;
    int READ_STORAGE_PERMISSION_CODE = 2;
    BottomNavigationView navbar;


        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == 1001) {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.music_songinfo_delete_successfully, Toast.LENGTH_SHORT).show();
                    restartActivity();
                } else {
                    Toast.makeText(this, R.string.music_songinfo_delete_error, Toast.LENGTH_SHORT).show();
                }
            }
        }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Prefs", "Checking setup status...");
        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        boolean setup_completed = prefs.getBoolean("setup_completed", false);
        if (!setup_completed) {
            Log.d("Prefs", "Setup isn't completed, launching activity!");
            Intent intent = new Intent(this, SetupActivity.class);
            startActivity(intent);
            finish();
        } else {
            Log.d("Prefs", "Setup completed, still here!");
            setContentView(R.layout.activity_main);
        }
        EdgeToEdge.enable(this);
        navbar = findViewById(R.id.main_navbar);
        if (navbar == null) {
            Log.e("MainActivity", "BottomNavigationView is null! Restarting activity...");
            restartActivity();
            return;
        }
        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("OPEN_NOWPLAYING", false)) {
                navbar.setSelectedItemId(R.id.id_navbar_nowplaying);
                changeFragment(new NowPlayingFragment());
            }
            else {
                navbar.setSelectedItemId(R.id.id_navbar_main);
                changeFragment(new HomeFragment());
            }
        }


        navbar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                navbar.getMenu().findItem(R.id.id_navbar_main).setIcon(R.drawable.navbar_home_outline);
                navbar.getMenu().findItem(R.id.id_navbar_nowplaying).setIcon(R.drawable.navbar_play_outline);
                navbar.getMenu().findItem(R.id.id_navbar_artist).setIcon(R.drawable.navbar_artist_outline);
                // navbar.getMenu().findItem(R.id.id_navbar_playlist).setIcon(R.drawable.navbar_playlist_outline);
                navbar.getMenu().findItem(R.id.id_navbar_settings).setIcon(R.drawable.navbar_settings_outline);

                if (item.getItemId() == R.id.id_navbar_nowplaying) {
                    item.setIcon(R.drawable.navbar_play_filled);
                    changeFragment(new NowPlayingFragment());
                }

                if (item.getItemId() == R.id.id_navbar_artist) {
                    item.setIcon(R.drawable.navbar_artist_filled);
                    changeFragment(new ArtistFragment());
                }

                if (item.getItemId() == R.id.id_navbar_main) {
                    item.setIcon(R.drawable.navbar_home_filled);
                    changeFragment(new HomeFragment());
                }
/*
                if (item.getItemId() == R.id.id_navbar_playlist) {
                    item.setIcon(R.drawable.navbar_playlist_filled);
                    changeFragment(new PlaylistFragment());
                }
*/
                if (item.getItemId() == R.id.id_navbar_settings) {
                    item.setIcon(R.drawable.navbar_settings_filled);
                    changeFragment(new SettingsFragment());
                }
                return true;
            }
        });

        navbar.getMenu().findItem(R.id.id_navbar_main).setIcon(R.drawable.navbar_home_filled);
        saveBasicPrefs();
//        getAllAudioFiles();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPermissionsGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_AUDIO) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.setup_permissionrequire)
                        .setMessage(R.string.main_needpermission)
                        .setCancelable(false)
                        .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(R.string.permission_grant, (dialog, which) -> requestStoragePermission())
                        .show();
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.permission_requirepermissionInSettingsTitle)
                        .setMessage(R.string.main_permissionsgotosettings)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(R.string.dialog_no, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }

    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 210124);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 190124);
        }
    }


    private void changeFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.main_frame_layout);

        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.main_frame_layout, fragment);
        ft.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 190124 || requestCode == 210924) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                restartActivity();
            }
        }
    }


/*
    public void changeFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        String fragmentTag = fragment.getClass().getName();
        Fragment existingFragment = fm.findFragmentByTag(fragmentTag);
        FragmentTransaction ft = fm.beginTransaction();
        if (existingFragment == null) {
            ft.add(R.id.main_frame_layout, fragment, fragmentTag);
        } else {
            ft.show(existingFragment);
        }
        for (Fragment frag : fm.getFragments()) {
            if (!frag.getTag().equals(fragmentTag) && frag.isVisible()) {
                ft.hide(frag);
            }
        }
        ft.commit();
    }
    */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, MusicService.class);
        stopService(serviceIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
    }


    public StringBuilder tracks = new StringBuilder();


    private void saveBasicPrefs() {
        // Base variables
        SharedPreferences sp = getSharedPreferences("userPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if(!sp.contains("appearance_nowplaying_trackCoverRound")) {
            Log.d("Prefs", "Saving track cover rounding...");
            editor.putInt("appearance_nowplaying_trackCoverRound", 16);
            editor.apply();
            Log.d("Prefs", "Track cover rounding saved!");

            // Save basic background color for track cover
            /*
            Log.d("Prefs", "Saving track cover background color...");
            editor.putString("appearance_nowplaying_trackCoverBackgroundColor", "#80b2b2b2");
            editor.apply();
            Log.d("Prefs", "Track cover background color saved!");
            Log.d("Prefs", "All basic prefs was created!");*/
        }
        else {
            Log.d("Prefs", "Basic prefs already created, skipping!");
        }
    }

    public boolean isPermissionsGranted() {
        boolean isStoragePermissionGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean isAudioPermissionGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                : isStoragePermissionGranted;
        return isAudioPermissionGranted;
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    public void SettingsOpenAppereance(View v) {
        Intent i = new Intent(this, SettingsAppearance.class);
        startActivity(i);
    }
}
