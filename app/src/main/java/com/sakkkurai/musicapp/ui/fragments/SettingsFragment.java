package com.sakkkurai.musicapp.ui.fragments;

import static android.content.Context.MODE_PRIVATE;
import static android.media.session.MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.ResultReceiver;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sakkkurai.musicapp.R;
import com.sakkkurai.musicapp.services.MusicService;
import com.sakkkurai.musicapp.ui.activities.SettingsAppearance;
import com.sakkkurai.musicapp.ui.activities.SetupActivity;

public class SettingsFragment extends Fragment {




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        TextView version = view.findViewById(R.id.settings_version);

        LinearLayout appearance = view.findViewById(R.id.settings_appearance);

        try {
            PackageManager pm = requireContext().getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(requireContext().getPackageName(), 0);


            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;

            version.setText(versionName + " (" + versionCode + ")");

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            version.setText(R.string.settings_versionError);
        }

        version.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                String[] debugMenuItems = getResources().getStringArray(R.array.debug_menu_items);

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
                builder.setTitle(R.string.debug_Title)
                        .setItems(debugMenuItems, ((dialogInterface, i) -> handleMenuAction(i)))
                        .setCancelable(true)
                        .setIcon(R.drawable.setup_info)
                        .setNegativeButton(R.string.debug_closeDialog, ((dialogInterface, i) -> dialogInterface.dismiss()));
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                builder.show();
                return false;
            }
        });
        return view;
    }


    public void SettingsOpenAppereance(View v) {
        Intent i = new Intent(getContext(), SettingsAppearance.class);
        startActivity(i);
    }

    private void handleMenuAction(int which) {
        switch (which) {
            case 0:
                Intent mainactivity = getActivity().getIntent();
                getActivity().finish();
                startActivity(mainactivity);
                break;
            case 1:
                Intent intent = new Intent(getContext(), SetupActivity.class);
                startActivity(intent);
                getActivity().finish();
                break;
            case 2:
                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                i.setData(uri);
                startActivity(i);
            case 3:
                SharedPreferences prefs = getActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                boolean advancedDebug = prefs.getBoolean("debug_use.extended.debug", false);
                if (advancedDebug) {
                    editor.putBoolean("debug_use.extended.debug", false);
                    editor.apply();
                    Toast.makeText(getActivity(), R.string.debug_use_extended_debug_disable, Toast.LENGTH_SHORT).show();
                }
                if (!advancedDebug) {
                    editor.putBoolean("debug_use.extended.debug", true);
                    editor.apply();
                    Toast.makeText(getActivity(), R.string.debug_use_extended_debug_enable, Toast.LENGTH_SHORT).show();
                }
                break;
            case 4:
                break;
        }
    }

}