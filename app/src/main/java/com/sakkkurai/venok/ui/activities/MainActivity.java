package com.sakkkurai.venok.ui.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
import com.sakkkurai.venok.services.MusicService;
import com.sakkkurai.venok.tools.AudioTools;
import com.sakkkurai.venok.tools.Updater;
import com.sakkkurai.venok.ui.fragments.ArtistFragment;
import com.sakkkurai.venok.ui.fragments.HomeFragment;
import com.sakkkurai.venok.ui.fragments.NowPlayingFragment;
import com.sakkkurai.venok.R;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sakkkurai.venok.ui.fragments.SettingsFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView navbar;
    private SharedPreferences.Editor temp_editor;
    public static int proccessingRemoveItemPosition;


        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode){
                case AudioTools.REQUEST_DELETE_SONG:
                    if (resultCode == Activity.RESULT_OK) {
                        Toast.makeText(this, R.string.music_songinfo_delete_successfully, Toast.LENGTH_SHORT).show();
                        FragmentManager fm = getSupportFragmentManager();
                        HomeFragment homeFragment = (HomeFragment) fm.findFragmentById(R.id.main_frame_layout);
                        if (homeFragment != null) {
                            Log.d("DeleteItem", "MainActivity: " + proccessingRemoveItemPosition);
                            homeFragment.removeTrackAt(proccessingRemoveItemPosition);
                        }

                    } else {
                        Toast.makeText(this, R.string.music_songinfo_delete_error, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case Updater.REQUEST_INSTALL_UNKNOWNSOURCES:
                    File apk = new File(getFilesDir(), Updater.apkName);
                    Updater.installApk(this, apk);
                    break;
            }
        }


    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        boolean setup_completed = prefs.getBoolean("setup_completed", false);
        SharedPreferences temp_prefs = getSharedPreferences("temp", MODE_PRIVATE);
        temp_editor = temp_prefs.edit();
        if (!setup_completed) {
            Intent intent = new Intent(this, SetupActivity.class);
            startActivity(intent);
            finish();
        } else {
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
            try {
                Updater.checkUpdates(this);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (getIntent().getBooleanExtra("OPEN_NOWPLAYING", false)) {
                navbar.setSelectedItemId(R.id.id_navbar_nowplaying);
                changeFragment(new NowPlayingFragment());
            }
            else {
                navbar.setSelectedItemId(R.id.id_navbar_main);
                changeFragment(new HomeFragment());
            }
        } else {
            int selectedItemId = savedInstanceState.getInt("selected_nav_item", R.id.id_navbar_main);
            navbar.setSelectedItemId(selectedItemId);
        }


        navbar.setOnItemSelectedListener(item -> {
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
        });

        int selectedItemId = navbar.getSelectedItemId();

        if (selectedItemId == R.id.id_navbar_nowplaying) {
            navbar.getMenu().findItem(R.id.id_navbar_nowplaying).setIcon(R.drawable.navbar_play_filled);
        } else if (selectedItemId == R.id.id_navbar_artist) {
            navbar.getMenu().findItem(R.id.id_navbar_artist).setIcon(R.drawable.navbar_artist_filled);
        } else if (selectedItemId == R.id.id_navbar_settings) {
            navbar.getMenu().findItem(R.id.id_navbar_settings).setIcon(R.drawable.navbar_settings_filled);
        } else {
            navbar.getMenu().findItem(R.id.id_navbar_main).setIcon(R.drawable.navbar_home_filled);
        }
        ServiceInit();

    }

    private void ServiceInit() {
            if (!MusicService.isRunning) {
                Intent i = new Intent(this, MusicService.class);
                i.putExtra("reason", "START_QUIET");
                startForegroundService(i);
            }
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
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(R.string.grant, (dialog, which) -> requestStoragePermission())
                        .show();
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.permission_requirepermissionInSettingsTitle)
                        .setMessage(R.string.main_permissionsgotosettings)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_nav_item", navbar.getSelectedItemId());
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
        Intent i = new Intent(this, MusicService.class);
        temp_editor.remove("LIBRARY_SCROLL_POSITION").commit();
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
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }
}
