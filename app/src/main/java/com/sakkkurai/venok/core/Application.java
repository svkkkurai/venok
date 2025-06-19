package com.sakkkurai.venok.core;


public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
//        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }
}
