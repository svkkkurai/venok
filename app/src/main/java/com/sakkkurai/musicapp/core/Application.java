package com.sakkkurai.musicapp.core;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
