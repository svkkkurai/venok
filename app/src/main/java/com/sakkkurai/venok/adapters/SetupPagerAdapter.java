package com.sakkkurai.venok.adapters;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.slider.Slider;
import com.sakkkurai.venok.R;
import com.sakkkurai.venok.ui.activities.SetupActivity;

public class SetupPagerAdapter extends RecyclerView.Adapter<SetupPagerAdapter.ViewHolder> {

    private final int[] layouts;
    private final ViewPager2 viewPager;
    public TextView seekbarText;
    public SeekBar seekbar;
    private final SetupActivity activity;
    public static final int READ_AUDIO_PERMISSION_CODE = 1;
    public static final int READ_STORAGE_PERMISSION_CODE = 2;
    private boolean isAnimationRunning = false;


    public ViewHolder holder;
    public Context context;


    public SetupPagerAdapter(int[] layouts, ViewPager2 viewPager, SetupActivity activity, Context context) {
        this.layouts = layouts;
        this.viewPager = viewPager;
        this.activity = activity;
        this.context = context;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layouts[viewType], parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        viewPager.setUserInputEnabled(true);
        if (position == 0) {
            TextView helloDesc = holder.itemView.findViewById(R.id.HelloDescription);
            String appName = context.getApplicationInfo().loadLabel(activity.getPackageManager()).toString();
            helloDesc.setText(appName + " " + context.getResources().getString(R.string.setup_description));
            Button setupGetStartedButton = holder.itemView.findViewById(R.id.setup_getstarted_button);
            if (setupGetStartedButton != null) {
                setupGetStartedButton.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
            Button getHelp = holder.itemView.findViewById(R.id.setup_privacy);
            getHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/1qye9mqPlDN_9112LAYIvR4ZYhQ9bEO7xQgxBYGYNB74/edit?usp=sharing"));
                    context.startActivity(intent);
                }
            });
            }
        }

        if (position == 1) {
            Button grant = holder.itemView.findViewById(R.id.setup_grant_button);
            ConstraintLayout c = holder.itemView.findViewById(R.id.setup_audio_perm_layout);
            LinearLayout scanLayout;
            scanLayout = holder.itemView.findViewById(R.id.setup_scan_layout);
            Log.d("s", String.valueOf(activity.isPermissionsGranted()));
            scanLayout.setVisibility(activity.isPermissionsGranted() ? View.VISIBLE : View.GONE);
            updatePermissionResultIcon(holder.itemView, scanLayout);
            TextView scanDesc = holder.itemView.findViewById(R.id.setup_scan_desc);
            scanDesc.setOnClickListener(v -> {
                activity.setUserDurationForScan(v);

            });
            if (c != null) {
                c.setOnClickListener(v -> {
                    if (activity.isPermissionsGranted() && !isAnimationRunning) {
                        updatePermissionResultIcon(holder.itemView, scanLayout);
                        isAnimationRunning = activity.isPermissionsGranted();
                    } else {
                        requestReadAudioPermission();
                    }
                });
            if (grant != null) {
                grant.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestReadAudioPermission();
                    }
                });
                boolean granted = activity.isPermissionsGranted();
                if (activity.isPermissionsGranted()) {

                    grant.setText(R.string.done);
                    grant.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            activity.endSetup();
                        }
                    });
                }
            }
            Slider seekbar;
            TextView seekbarmax;
            TextView seekbarText;

            seekbar = holder.itemView.findViewById(R.id.setup_seekbar);
            seekbarmax = holder.itemView.findViewById(R.id.setup_seekbarmax);
            seekbarText = holder.itemView.findViewById(R.id.setup_seekbarcurrent);


            SharedPreferences sp = activity.getSharedPreferences("userPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            int progress = sp.getInt("scanfrom", activity.getResources().getInteger(R.integer.scanfrom));
                if (seekbar != null && seekbarText != null) {
                    seekbar.setValue(progress);
                    seekbarText.setText(String.valueOf(progress));

                    seekbar.addOnChangeListener(new Slider.OnChangeListener() {
                        @Override
                        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                            seekbarText.setText(String.valueOf((int) value));
                        }
                    });
                    seekbar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                        @Override
                        public void onStartTrackingTouch(@NonNull Slider slider) {

                        }

                        @Override
                        public void onStopTrackingTouch(@NonNull Slider slider) {
                            editor.putInt("scanfrom", (int) seekbar.getValue());
                            editor.apply();
                        }
                    });
                }
            }
        }
    }

    private void requestReadAudioPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        int requestCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? READ_AUDIO_PERMISSION_CODE
                : READ_STORAGE_PERMISSION_CODE;

        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
    }

    public void updatePermissionResultIcon(View itemView, LinearLayout layout) {
        if (!isAnimationRunning) {
            isAnimationRunning = activity.isPermissionsGranted();
            ImageView permissionResultImage = itemView.findViewById(R.id.setup_permissionResult);
            if (permissionResultImage == null) return;

            String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_AUDIO
                    : Manifest.permission.READ_EXTERNAL_STORAGE;

            int newIcon = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
                    ? R.drawable.setup_perm_given
                    : R.drawable.setup_perm_not_given;
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(permissionResultImage, "scaleX", 1f, 0.5f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(permissionResultImage, "scaleY", 1f, 0.5f);
            scaleDownX.setDuration(200);
            scaleDownY.setDuration(200);
            scaleDownX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    permissionResultImage.setImageResource(newIcon);
                }
            });

            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(permissionResultImage, "scaleX", 0.5f, 1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(permissionResultImage, "scaleY", 0.5f, 1f);
            scaleUpX.setDuration(200);
            scaleUpY.setDuration(200);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(scaleDownX).with(scaleDownY);
            animatorSet.play(scaleUpX).with(scaleUpY).after(scaleDownX);
            animatorSet.start();
            if (activity.isPermissionsGranted()) {
                layout.setVisibility(activity.isPermissionsGranted() ? View.VISIBLE : View.GONE);
                layout.setAlpha(0f);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(layout, "alpha", 0f, 1f);
                fadeIn.setDuration(500);
                fadeIn.start();
            }
        }

    }




    @Override
    public int getItemCount() {
        return layouts.length;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public void setSeekbarProgress(int progress) {
        int position = 1; // Позиция страницы с SeekBar (если это фиксировано)
        notifyItemChanged(position, progress);
    }

    public int getSeekbarValue() {
        if (seekbar != null) {
            return seekbar.getProgress();
        }
        else {
            return 0;
        }
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
