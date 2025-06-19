package com.sakkkurai.venok.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.sakkkurai.venok.R;
import com.sakkkurai.venok.adapters.SetupPagerAdapter;

public class SetupActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private SetupPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);
        int[] layouts = {R.layout.setup_page1, R.layout.setup_page2};
        viewPager = findViewById(R.id.viewpager);
        adapter = new SetupPagerAdapter(layouts, viewPager, this, this);
        viewPager.setAdapter(adapter);
    }

    public void setUserDurationForScan(View v) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.setup_scancustom_input, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.setup_userdurationscan_title);
        builder.setCancelable(true);

        TextInputEditText duration_input = dialogView.findViewById(R.id.number_input);

        builder.setView(dialogView);
        builder.setMessage(R.string.setup_userdurationscan);
        builder.setNegativeButton(R.string.setup_discard, (dialogInterface, i) -> dialogInterface.cancel());

        builder.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            if (duration_input != null) {
                String value = duration_input.getText().toString().trim();
                if (!value.isEmpty()) {
                    try {
                        int duration = Integer.parseInt(value);
                        SharedPreferences sp = getSharedPreferences("userPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putInt("userScanFromDuration", duration);
                        editor.apply();
                        adapter.setSeekbarProgress(duration);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.setup_scan_enternormalvalue, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.setup_scan_isStringEmpty, Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.show();
    }



    public boolean isPermissionsGranted() {
        boolean isStoragePermissionGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean isAudioPermissionGranted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                : isStoragePermissionGranted;
        return isAudioPermissionGranted;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    public void endSetup() {
        SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("setup_completed", true);
        editor.apply();
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void openAppSettings(View v) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            boolean permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (permissionGranted) {
                viewPager.setCurrentItem(2, true); // Move to the 3rd page (index 2)
                viewPager.setUserInputEnabled(true); // Disable swipe back
                updatePermissionStatus();
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    showSettingsRedirectDialog();
                } else {
                    showPermissionExplanationDialog(permissions[0], requestCode);
                }
            }
        }
    }

    private void updatePermissionStatus() {
        if (viewPager.getAdapter() instanceof SetupPagerAdapter) {
            ((SetupPagerAdapter) viewPager.getAdapter()).notifyDataSetChanged();
        }
    }

    private void showSettingsRedirectDialog() {
        new MaterialAlertDialogBuilder(SetupActivity.this)
                .setCancelable(false)
                .setTitle(R.string.permission_requirepermissionInSettingsTitle)
                .setMessage(R.string.permission_requirepermissionInSettings)
                .setPositiveButton(R.string.gotosettings, (dialogInterface, i) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + SetupActivity.this.getPackageName()));
                    SetupActivity.this.startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }



    private void showPermissionExplanationDialog(String permission, int requestCode) {
        new MaterialAlertDialogBuilder(SetupActivity.this)
                .setCancelable(false)
                .setTitle(R.string.permission_requirepermissionTitle)
                .setMessage(R.string.permission_requirepermission)
                .setPositiveButton(R.string.grant, (dialogInterface, i) -> ActivityCompat.requestPermissions(SetupActivity.this, new String[]{permission}, requestCode))
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

}
