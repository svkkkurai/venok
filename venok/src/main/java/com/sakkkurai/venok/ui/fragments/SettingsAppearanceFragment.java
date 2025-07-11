package com.sakkkurai.venok.ui.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.sakkkurai.venok.R;

public class SettingsAppearanceFragment extends Fragment {

    private Slider np_round_sb;
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
        /*requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.appearance_frame, new NowPlayingFragment()).commit();*/


        SharedPreferences prefs = getActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
        int np_savedround = prefs.getInt("cornerradius", getResources().getInteger(R.integer.cornerradius));

        np_round_sb.setValue(np_savedround);
        np_current_round.setText(String.valueOf(np_savedround));
        np_round_sb.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                np_current_round.setText(String.valueOf( (int) np_round_sb.getValue()));
            }
        });
        np_round_sb.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
           @Override
           public void onStartTrackingTouch(@NonNull Slider slider) {

           }

           @Override
           public void onStopTrackingTouch(@NonNull Slider slider) {
               SharedPreferences sp = getActivity().getSharedPreferences("userPrefs", MODE_PRIVATE);
               SharedPreferences.Editor editor = sp.edit();
               editor.putInt("cornerradius", (int) np_round_sb.getValue());
               editor.apply();

               Log.d("Prefs", "Saved! Cover corner radius " + String.valueOf(np_round_sb.getValue()));
           }
       });
        return v;
    }
}