package com.sakkkurai.musicapp.core;


import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.sakkkurai.musicapp.BuildConfig;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
//        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }
}
